/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vzw.util;

import java.util.LinkedList;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author hud
 */
public class XmlUtil {

	public static class Attribute {
		private String			name = null;
		private String			value = null;

		public Attribute(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}


	}

	public static class Element {
		private String			tag = null;
		private Attribute[]		attrs = null;
		private Object[]		subElements = null;
		private String			text = null;

		public Element(String tag, String text, Attribute[] attrs, Object[] subElements) {
			this.tag = tag;
			this.attrs = attrs;
			this.text = text;
			this.subElements = subElements;
		}

		public Element(String tag, Attribute[] attrs, Object[] subElements) {
			this(tag, null, attrs, subElements);
		}

		public Element(String tag, String text) {
			this(tag, text, null, null);
		}

		public Element(String tag, Object[] subElements) {
			this(tag, null, null, subElements);
		}


		public String getTag() {
			return tag;
		}

		public Attribute[] getAttrs() {
			return attrs;
		}

		public Object[] getSubElements() {
			return subElements;
		}

		public String getText() {
			return text;
		}


	}



	public static void addElement(StringBuilder sb, Element element) {
		sb.append('<').append(element.getTag());

		// attributes
		if (element.getAttrs() != null) {
			for (Attribute attr : element.getAttrs()) {
				sb.append(' ')
				  .append(attr.getName()).append("=\"")
				  .append(StringEscapeUtils.escapeXml(attr.getValue()))
				  .append("\"");
			}
		}

		sb.append('>');


		// sub-elements
		if (element.getSubElements() != null) {
			for (Object subElement : element.getSubElements()) {

				if (subElement instanceof Element) {
					addElement(sb, (Element)subElement);
				}
				else if (subElement instanceof Element[]) {
					for (Element se : (Element[])subElement) {
						addElement(sb, se);
					}
				}

			}
		}

		// text
		if (element.getText() != null) {
			sb.append(StringEscapeUtils.escapeXml(element.getText()));
		}

		sb.append("</").append(element.getTag()).append('>');
	}


	public static class Stringer {

		private static class Tag {
			private static enum State {
				startOpen,
				startClose,
				done
			}

			private boolean		hasContent = false;
			private String		tagName;
			private State		state = State.startOpen;

			private Tag(String tagName) {
				this.tagName = tagName;
				
			}

			public boolean hasContent() {
				return hasContent;
			}

			public void setHasContent(boolean hasContent) {
				this.hasContent = hasContent;
			}

			

		}

		private StringBuilder	sbBuffer = null;
		private LinkedList<Tag>	tagStack = new LinkedList<Tag>();

		public Stringer() {
			sbBuffer = new StringBuilder();
		}

		public Stringer(int capacity) {
			sbBuffer = new StringBuilder(capacity);
		}
		
		public Stringer(int capacity, boolean addHeader) {
			this(capacity);
			
			if (addHeader) {
				sbBuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			}
		}


		public Stringer tag(String tagName) {
			// check if the previous tag is open
			Tag tagLast = tagStack.peek();
			if (tagLast != null) {
				switch (tagLast.state) {
					case startOpen:
						// it's a parent tag, close it
						sbBuffer.append('>');
						tagLast.state = Tag.State.startClose;
						tagLast.setHasContent(true);
						break;

					case done:
						// it's an error
						throw new IllegalArgumentException("Invalid tag insert");
						

					case startClose:
						// do nothing, it's closed by a sibling
						break;
				}
			}

			sbBuffer.append('<').append(tagName);
			tagStack.push(new Tag(tagName));
			return this;
		}

		public Stringer endTag() {
			Tag tagLast = tagStack.poll();
			if (tagLast == null) {
				throw new IllegalArgumentException("Unable to end tag, no tag found");
			}
			switch (tagLast.state) {
				case startOpen:
					// no attributes done, no sub-elements close it
					if (tagLast.hasContent) {
						sbBuffer.append('>');
					}
					break;

				case done:
					// it's an error
					throw new IllegalArgumentException("Invalid tag with done state");

				case startClose:
					break;
			}

			if (tagLast.hasContent()) {
				sbBuffer.append("</").append(tagLast.tagName).append('>');
			}
			else {
				sbBuffer.append("/>");
			}

			tagLast.state = Tag.State.done;

			return this;

		}

		public Stringer attr(String attrName, String attrValue) {
			Tag tagLast = tagStack.peek();
			if (tagLast == null) {
				throw new IllegalArgumentException("Unable to add new attribute, no tag found");
			}

			switch (tagLast.state) {
				case startOpen:
					sbBuffer.append(' ').append(attrName)
							.append("=\"").append(StringEscapeUtils.escapeXml(attrValue))
							.append('\"');
					break;

				default:
					throw new IllegalArgumentException("Unable to add attributes, tag is closed");
			}

			return this;
		}

		public Stringer text(String text) {
			Tag tagLast = tagStack.peek();
			if (tagLast == null) {
				throw new IllegalArgumentException("Unable to add text, no tag found");
			}

			switch (tagLast.state) {
				case startOpen:
					// close it
					sbBuffer.append('>');
					tagLast.state = Tag.State.startClose;
					tagLast.setHasContent(true);
					break;

				case done:
					throw new IllegalArgumentException("Unable to add text, tag is closed");

			}

			sbBuffer.append(StringEscapeUtils.escapeXml(text));
			
			return this;
		}


		/**
		 * dummy method to facilitate easy calls
		 * @param obj
		 * @return
		 */
		public Stringer external(Object obj) {
			return this;
		}



		@Override
		public String toString() {
			return sbBuffer.toString();
		}

	}
}
