/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.util.Precision;
import org.apache.log4j.Logger;

/**
 *
 * @author hud
 * 
 * Simple image manipulation function
 */
public class ImageUtil {
	private static final Logger				logger = Logger.getLogger(ImageUtil.class);
	
	public static final class ResizeRes {
		public Dim			dim = null;
		public boolean		copyRequired = false;	// if this is true, the caller needs to copy the file, the resize did nothing!
	}
	
	public static final class iRGBA {
		private int		blue = 0;
		private int		green = 0;
		private int		red = 0;
		private int		alpha = 255;
		
		public iRGBA() {
			
		}
		
		public iRGBA(int red, int green, int blue, int alpha) {
			set(red, green, blue, alpha);
		}
		
		public iRGBA(int red, int green, int blue) {
			this(red, green, blue, 255);
		}
		
		/**
		 * 
		 * @param htmlColor 
		 *		#AABBCC or AABBCC
		 */
		public iRGBA(String htmlColor) throws Exception {
			try {
				if (htmlColor != null) {
					int val = Integer.decode(htmlColor);
					blue = (val & 0xFF);
					val >>= 8;
					green = (val & 0xFF);
					val >>= 8;
					red = (val & 0xFF);
				}
			}
			catch (Throwable t) {
				LogUtil.errorAndThrow(logger, t, 
						new SimpleException(t, "Invalid htmlColor for iRGBA: {0}", htmlColor));
			}
		}
		
		/**
		 * in iRGBA format
		 * @param color 
		 */
		public iRGBA(int[] color) {
			this(color[0], color[1], color[2], color[3]);
		}
		
		public iRGBA set(int[] color) {
			return set(color[0], color[1], color[2], color[3]);
		}
		
		public iRGBA set(int[] colorArray, int startIndex) {
			return set(colorArray[startIndex], colorArray[startIndex + 1], colorArray[startIndex + 2], colorArray[startIndex + 3]);
		}
		
		public boolean equals(int[] colorArray, int startIndex) {
			if (colorArray[startIndex] != red) {
				return false;
			}
			if (colorArray[startIndex + 1] != green) {
				return false;
			}
			if (colorArray[startIndex + 2] != blue) {
				return false;
			}
			if (colorArray[startIndex + 3] != alpha) {
				return false;
			}
			
			return true;
		}
		
		
		public boolean equalsWithoutAlpha(int[] colorArray, int startIndex) {
			if (colorArray[startIndex] != red) {
				return false;
			}
			if (colorArray[startIndex + 1] != green) {
				return false;
			}
			if (colorArray[startIndex + 2] != blue) {
				return false;
			}
			return true;
		}
		
		public boolean lowLikeWithoutAlpha(int[] colorArray, int startIndex, int lowDiff) {
			if (colorArray[startIndex] < red - lowDiff) {
				return false;
			}
			if (colorArray[startIndex + 1] < green - lowDiff) {
				return false;
			}
			if (colorArray[startIndex + 2] < blue - lowDiff) {
				return false;
			}
			
			return true;
		}
		
		
		
		public void copyTo(int[] colorArray, int startIndex) {
			colorArray[startIndex] = red;
			colorArray[startIndex + 1] = green;
			colorArray[startIndex + 2] = blue;
			colorArray[startIndex + 3] = alpha;
		}
		
		
		
		/**
		 * 
		 * @param srcArray
		 * @param dstArray 
		 */
		public static void copy(int[] srcArray, int srcOffset, int[] dstArray, int dstOffet) {
			for (int i = 0; i < 4; ++ i) {
				dstArray[dstOffet + i] = srcArray[srcOffset + i];
			}
		}
		
		/**
		 * src over dst ([0,255]) range
		 * 
		 * @param srcArray
		 * @param srcOffset
		 * @param dstArray
		 * @param dstOffset 
		 */
		public static void alphaOver(int[] srcArray, int srcOffset, int[] dstArray, int dstOffset) {
			double srcAlpha = srcArray[srcOffset + 3] / 255.0;
			double dstAlpha = dstArray[dstOffset + 3] / 255.0;
			double srcAlphaR = 1 - srcAlpha;
			
			for (int i = 0; i < 3; ++ i) {
				dstArray[dstOffset + i] = (int)Math.round(srcArray[srcOffset + i] * srcAlpha + dstArray[dstOffset + i] * dstAlpha * srcAlphaR);
			}
			
			dstArray[dstOffset + 3] = (int)Math.round(255 * (srcAlpha + dstAlpha * srcAlphaR));
		}
		
		/**
		 * a over b
		 * @param a
		 * @param b
		 * @return 
		 */
		public static iRGBA alphaOver(iRGBA a, iRGBA b) {
			iRGBA res = new iRGBA();
			return alphaOver(a, b, res);
		}
		
		
		/**
		 * return res
		 * @param a
		 * @param b
		 * @param res
		 * @return 
		 */
		public static iRGBA alphaOver(iRGBA a, iRGBA b, iRGBA res) {
			double alphaA = a.getAlpha() / 255.0;
			double alphaB = b.getAlpha() / 255.0;
			double alphaAR = 1 - alphaA;
			
			
			res.setRed((int)Math.round(a.getRed() * alphaA + b.getRed() * alphaB * alphaAR));
			res.setGreen((int)Math.round(a.getGreen() * alphaA + b.getGreen() * alphaB * alphaAR));
			res.setBlue((int)Math.round(a.getBlue() * alphaA + b.getBlue() * alphaB * alphaAR));
			res.setAlpha((int)Math.round(255 * (alphaA + alphaB * alphaAR)));
			
			return res;
			
		}
		
		public static void alphaOver(
				int[]		dataSrc, 
				int			offsetSrc, 
				iRGBA		colorb,
				int[]		dataDst,
				int			offsetDst
		) {
			double alphaA = dataSrc[offsetSrc + 3] / 255.0;
			double alphaB = colorb.alpha / 255.0;
			double alphaAR = 1 - alphaA;
			
			dataDst[offsetDst] = ((int)Math.round(dataSrc[offsetSrc] * alphaA + colorb.getRed() * alphaB * alphaAR));
			dataDst[offsetDst + 1] = ((int)Math.round(dataSrc[offsetSrc + 1] * alphaA + colorb.getGreen() * alphaB * alphaAR));
			dataDst[offsetDst + 2] = ((int)Math.round(dataSrc[offsetSrc + 2] * alphaA + colorb.getBlue() * alphaB * alphaAR));
			dataDst[offsetDst + 3] = ((int)Math.round(255 * (alphaA + alphaB * alphaAR)));
			
		}
		
		/**
		 * 
		 * @param data
		 * @param offset
		 * @return 
		 */
		public static boolean isNotZero(int[] data, int offset) {
			return data[offset] != 0 ||
					data[offset + 1] != 0 ||
					data[offset + 2] != 0 ||
					data[offset + 3] != 0;
		}
		
		/**
		 * 
		 * @param data
		 * @param offset
		 * @return 
		 */
		public static boolean isZero(int[] data, int offset) {
			return data[offset] == 0 &&
					data[offset + 1] == 0 &&
					data[offset + 2] == 0 &&
					data[offset + 3] == 0;
		}
		
		/**
		 * 
		 * @param data
		 * @param offset
		 * @return 
		 */
		public static boolean isGray(int[] data, int offset) {
			return data[offset] == data[offset + 1] &&
					data[offset] == data[offset + 2];
		}

		/**
		 * 
		 * @param srcColor
		 * @param val
		 * @return 
		 */
		public static String mul(String srcColor, double val) {
			
			String ret = null;
			try {
				iRGBA src = new iRGBA(srcColor);
				iRGBA dst = src.mul(val);
				ret = dst.toHtmlColor();
			}
			catch (Exception e) {
				LogUtil.error(logger, e, "Failed to mul color: src={0}, mulval={1}", srcColor, val);
			}
				
			return ret;
		}
		
		// no alpha
		/**
		 * 
		 * @param val
		 * @return 
		 */
		public iRGBA mul(double val) {
			iRGBA dst = new iRGBA();
			dst.setRed(normalizeColorValue((int)Math.round(red * val)));
			dst.setGreen(normalizeColorValue((int)Math.round(green * val)));
			dst.setBlue(normalizeColorValue((int)Math.round(blue * val)));
			dst.setAlpha(alpha);
			
			return dst;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final iRGBA other = (iRGBA) obj;
			if (this.blue != other.blue) {
				return false;
			}
			if (this.green != other.green) {
				return false;
			}
			if (this.red != other.red) {
				return false;
			}
			if (this.alpha != other.alpha) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 23 * hash + this.blue;
			hash = 23 * hash + this.green;
			hash = 23 * hash + this.red;
			hash = 23 * hash + this.alpha;
			return hash;
		}
		
		public int toInt() {
			return (alpha << 24) | (red << 16) | (green << 8) | blue;
		}
		
		/**
		 * Only pick RGB
		 */
		public String toHtmlColor() {
			return String.format("#%1$02X%2$02X%3$02X", 
				red, green, blue);
		}
		
		public final iRGBA set(int red, int green, int blue, int alpha) {
			this.red = red;
			this.green = green;
			this.blue = blue;
			this.alpha = alpha;
			return this;
		}

		public int getAlpha() {
			return alpha;
		}

		public void setAlpha(int alpha) {
			this.alpha = alpha;
		}

		public int getBlue() {
			return blue;
		}

		public void setBlue(int blue) {
			this.blue = blue;
		}

		public int getGreen() {
			return green;
		}

		public void setGreen(int green) {
			this.green = green;
		}

		public int getRed() {
			return red;
		}

		public void setRed(int red) {
			this.red = red;
		}
		
		
	}

	
	public static final class Ratio {
		private double rx = 1;
		private double ry = 1;
		
		public Ratio() {
			
		}
		
		public Ratio(double rx, double ry) {
			set(rx, ry);
		}
		
		public boolean isOne() {
			return Precision.equals(rx, 1., 1e-4) && Precision.equals(ry, 1., 1e-4);
		}
		
		public void set(double rx, double ry) {
			this.rx = rx;
			this.ry = ry;
		}

		public double getRx() {
			return rx;
		}

		public void setRx(double rx) {
			this.rx = rx;
		}

		public double getRy() {
			return ry;
		}

		public void setRy(double ry) {
			this.ry = ry;
		}
		
		public void set(Ratio r) {
			this.rx = r.getRx();
			this.ry = r.getRy();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Ratio other = (Ratio) obj;
			if (Double.doubleToLongBits(this.rx) != Double.doubleToLongBits(other.rx)) {
				return false;
			}
			if (Double.doubleToLongBits(this.ry) != Double.doubleToLongBits(other.ry)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 67 * hash + (int) (Double.doubleToLongBits(this.rx) ^ (Double.doubleToLongBits(this.rx) >>> 32));
			hash = 67 * hash + (int) (Double.doubleToLongBits(this.ry) ^ (Double.doubleToLongBits(this.ry) >>> 32));
			return hash;
		}
		
		
	}
	
	public static final class Dim {
		private int width = 0;
		private int height = 0;

		public Dim() {
		}
		
		public Dim(int width, int height) {
			set(width, height);
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}
		
		public void set(int width, int height) {
			this.width = width;
			this.height = height;
		}
		
		public void set(double width, double height) {
			this.width = (int)Math.round(width);
			this.height = (int)Math.round(height);
		}
		
		
		public void set(Dim srcDim) {
			this.width = srcDim.getWidth();
			this.height = srcDim.getHeight();
		}
		
		
		/**
		 * 
		 * @param jo
		 * @return 
		 */
		public static Dim fromJson(JSONObject jo) {
			Dim dim = JSONUtil.toJava(jo, Dim.class);
			return dim;
		}

		/**
		 * 
		 * @param joStr
		 * @return 
		 */
		public static Dim fromJson(String joStr) {
			return JSONUtil.toJava(joStr, Dim.class);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Dim other = (Dim) obj;
			if (this.width != other.width) {
				return false;
			}
			if (this.height != other.height) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 41 * hash + this.width;
			hash = 41 * hash + this.height;
			return hash;
		}
		
		
	}
	
	
	public static class ResizeParam {
		private Dim		dim = null;
		private Ratio	ratio = null;
		
		public ResizeParam() {
			
		}
		
		public ResizeParam(Dim dim, Ratio ratio) {
			this.dim = dim;
			this.ratio = ratio;
		}

		public Dim getDim() {
			return dim;
		}

		public void setDim(Dim dim) {
			this.dim = dim;
		}

		public Ratio getRatio() {
			return ratio;
		}

		public void setRatio(Ratio ratio) {
			this.ratio = ratio;
		}
		
		
	}
	
	static {
		//System.setProperty("jmagick.systemclassloader", "no");
		
		// list supported image types
		String[] readerTypes = ImageIO.getReaderFormatNames();
		boolean first = true;
		StringBuilder sbMsg = new StringBuilder();
		for (String t : readerTypes) {
			if (first) {
				sbMsg.append("ImageUtil over ImageIO: supported image read types: ");
			}
			
			if (!first) {
				sbMsg.append(",");
			}
			
			sbMsg.append(t);
			
			first = false;
		}
		LogUtil.info(logger, sbMsg.toString());
		
		String[] writerTypes = ImageIO.getWriterFormatNames();
		sbMsg.delete(0, sbMsg.length());
		for (String t : writerTypes) {
			if (first) {
				sbMsg.append("ImageUtil over ImageIO: supported image write types: ");
			}
			
			if (!first) {
				sbMsg.append(",");
			}
			
			sbMsg.append(t);
			
			first = false;
		}
		LogUtil.info(logger, sbMsg.toString());
		
	}
	
	public static int normalizeColorValue(int c) {
		return c > 255 ? 255 : (c < 0 ? 0 : c);
	}
	
	/**
	 * Only support same type resize, so outFile's extension must be same as inFile's
	 * @param inFile
	 * @param outFile
	 * @param dstWidth
	 * @param dstHeight
	 * @param keepAspectRatio
	 * @param dstQuality 
	 *		default to 50
	 */
	public static Dim resize(File inFile, File outFile,
			int dstWidth, int dstHeight, boolean keepAspectRatio, boolean upTo)
	throws Exception
	{
		return resize(inFile, outFile, dstWidth, dstHeight, keepAspectRatio, upTo, 50);
	}
	
	/**
	 * 
	 * @param inPath
	 * @param outPath
	 * @param dstWidth
	 * @param dstHeight
	 * @param keepAspectRatio
	 * @throws Exception 
	 */
	public static Dim resize(String inPath, String outPath,
			int dstWidth, int dstHeight, boolean keepAspectRatio, boolean upTo)
	throws Exception {
		return resize(new File(inPath), new File(outPath), dstWidth, dstHeight, keepAspectRatio, upTo);
	}	
	
	/**
	 * 
	 * @param inFile
	 * @param outFile
	 * @param dstWidth
	 * @param dstHeight
	 * @param keepAspectRatio
	 * @param dstQuality
	 * @throws Exception 
	 */
	public static Dim resize(File inFile, File outFile,
			int dstWidth, int dstHeight, boolean keepAspectRatio,
			boolean upTo, int dstQuality) 
	throws Exception {
		return resize(inFile, outFile, null,
				dstWidth, dstHeight, keepAspectRatio, upTo, dstQuality);
	}
	
	/**
	 * 
	 * @param inPath
	 * @param outPath
	 * @param dstWidth
	 * @param dstHeight
	 * @param keepAspectRatio
	 * @param dstQuality
	 * @throws Exception 
	 */
	/*
	public static Dim resize(String inPath, String outPath,
			int dstWidth, int dstHeight, boolean keepAspectRatio,
			boolean upTo, int dstQuality)
	throws Exception {
		
		MagickImage miIn = null;
		MagickImage miOut = null;
		Dim ret = new Dim();
		try {
			boolean doCopy = false;
			ImageInfo iiIn = new ImageInfo(inPath);
			miIn = new MagickImage(iiIn);
			Dimension dimIn = miIn.getDimension();

			int dstWidth1, dstHeight1;

			double srcWidth = dimIn.getWidth();
			double srcHeight = dimIn.getHeight();
			if (keepAspectRatio) {
				double srcyx = srcHeight / srcWidth;
				double dstyx = dstHeight / dstWidth;
				if (dstyx > srcyx) {
					dstWidth1 = dstWidth;
					dstHeight1 = (int)Math.round(dstWidth / srcWidth * srcHeight);
				}
				else {
					dstHeight1 = dstHeight;
					dstWidth1 = (int)Math.round(dstHeight / srcHeight * srcWidth);
				}
			}
			else {
				dstWidth1 = dstWidth;
				dstHeight1 = dstHeight;
			}
			
			if (upTo) {
				if (dstWidth1 > srcWidth || dstHeight1 > srcHeight) {
					// do nothing but copy
					doCopy = true;
				}
			}
			
			if (doCopy) {
				FileUtils.copyFile(new File(inPath), new File(outPath), true);
				ret.set(srcWidth, srcHeight);
			}
			else {
			
			// do not do animated gif for now
			//@
			if (miIn.isAnimatedImage()) {
				MagickImage[] frames = miIn.breakFrames();
				MagickImage[] dstFrames = new MagickImage[frames.length];
				
			
				for (int i = 0; i < frames.length; ++ i) {
					dstFrames[i] = frames[i].scaleImage(dstWidth1, dstHeight1);
				}

				miOut = new MagickImage(dstFrames);
			}
			else {//@/
				miOut = miIn.scaleImage(dstWidth1, dstHeight1);
			//}
			
				miOut.setFileName(outPath);

				ImageInfo iiOut = new ImageInfo();
				iiOut.setQuality(dstQuality);
				miOut.writeImage(iiOut);
				
				ret.set(dstWidth1, dstHeight1);
			}
		}
		catch (Exception e) {
			LogUtil.error(logger, e, "Failed to resize image");
			throw e;
		}
		finally {
			if (miIn != null) {
				miIn.destroyImages();
			}
			
			if (miOut != null) {
				miOut.destroyImages();
			}
		}
		
		return ret;
		
	}
	*/
	

	/**
	 * 
	 * @param inFile
	 * @param outFile
	 * @param formatName
	 * @param dstWidth
	 * @param dstHeight
	 * @param keepAspectRatio
	 * @param upTo
	 * @param dstQuality
	 * @return
	 * @throws ResizeImageFailureException 
	 */
	public static Dim resize(File inFile, File outFile, String dstFormatName,
			int dstWidth, int dstHeight, boolean keepAspectRatio, boolean upTo, int dstQuality)
	throws ResizeImageFailureException {

		String dstFormatName1 = dstFormatName;
		if (dstFormatName1 == null) {
			dstFormatName1 = FilenameUtils.getExtension(outFile.getName());
		}
		
		InputStream in = null;
		OutputStream out = null;
		
		Dim ret = null;
		
		try {
			
			in = new FileInputStream(inFile);
			out = new FileOutputStream(outFile);
			
			ResizeRes res = resize(in, out, dstFormatName1, dstWidth, dstHeight, keepAspectRatio, upTo, dstQuality);
			if (res.copyRequired) {
				FileUtils.copyFile(inFile, outFile);
			}
		}
		catch (IOException ioe) {
			LogUtil.errorAndThrow(logger, ioe, 
					new ResizeImageFailureException(ioe, "File not found when resizing image"));
		}
		finally {
			CleanupUtil.release(in, out);
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param iis
	 * @return 
	 */
	public static ImageReader getImageReader(ImageInputStream iis) {
		Iterator<ImageReader> ir = ImageIO.getImageReaders(iis);
		if (ir == null || !ir.hasNext()) {
			return null;
		}
		else {
			return ir.next();
		}
	}
	
	public static ImageReader getImageReader(InputStream is) throws IOException {
		ImageInputStream iis = ImageIO.createImageInputStream(is);
		return getImageReader(iis);
	}
	
	public static ImageWriter getImageWriter(String formatName) {
		Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName);
		if (iw == null || !iw.hasNext()) {
			return null;
		}
		else {
			return iw.next();
		}
	}
	
	public static ImageWriter getImageWriter(MimeType mimeType) {
		return getImageWriter(mimeType.getExt());
	}
	
	public static boolean isSupportedFormat(MimeType mimeType) {
		return mimeType == MimeType.GIF ||
				mimeType == MimeType.JPG ||
				mimeType == MimeType.PNG ||
				mimeType == MimeType.BMP;
	}
	

	
	/**
	 * 
	 * dstFormatName is usually extension of an image file, so far only png, jpg, gif are supported
	 * dstQuality may not be used for the moment
	 * 
	 * @param in
	 * @param out
	 * @param dstFormatName1
	 * @param dstWidth
	 * @param dstHeight
	 * @param keepAspectRatio
	 * @param upTo
	 * @param dstQuality
	 * @throws Exception 
	 */
	public static ResizeRes resize(
		InputStream				in, 
		OutputStream			out, 
		String					dstFormatName,
		int						dstWidth, 
		int						dstHeight, 
		boolean					keepAspectRatio,
		boolean					upTo,
		int						dstQuality) 
	throws ResizeImageFailureException {
		
		ResizeRes ret = new ResizeRes();
		ResizeParam rp = null;
		
		ImageReader ir = null;
		ImageWriter iw = null;
		ImageInputStream iis = null;
		ImageOutputStream ios = null;
		try {
			MimeType dstMimeType = MimeType.fromExt(dstFormatName);
			
			// verify destination format name
			if (!isSupportedFormat(dstMimeType)) {
				LogUtil.errorAndThrow(logger, new ResizeImageFailureException("Unsupported destination format: {0}", dstFormatName));
			}
			

			// prepare source
			iis = ImageIO.createImageInputStream(in);
			ir = getImageReader(iis);
			if (ir == null) {
				LogUtil.errorAndThrow(logger, new ResizeImageFailureException("Unable to get ImageReader for the source image"));
			}
			String srcFormatName = ir.getFormatName();
			MimeType srcMimeType = MimeType.fromExt(srcFormatName);
			
			// verify source format name
			if (!isSupportedFormat(srcMimeType)) {
				LogUtil.errorAndThrow(logger, new ResizeImageFailureException("Unsupported source format format: {0}", srcFormatName));
			}
			
			
			//
			// if dst is gif but source is not, do not support
			//
			if (dstMimeType == MimeType.GIF && srcMimeType != MimeType.GIF) {
				LogUtil.errorAndThrow(logger, new ResizeImageFailureException("Source format is {0} while destination format is {1}", srcFormatName, dstFormatName));
			}
			
			
			// set input
			ir.setInput(iis);
			
			
			// do resize
			if (dstMimeType == MimeType.GIF && srcMimeType == MimeType.GIF) {
				// only process gif
				rp = resizeGif(in, out, ir, new Dim(dstWidth, dstHeight), keepAspectRatio, upTo);
			}
			else {
				
				BufferedImage inImage = ir.read(0);	// no matter what, only read from zero
				int srcWidth = inImage.getWidth();
				int srcHeight = inImage.getHeight();
				
				// other formats (png & jpg)
				int dstType = -1;
				switch (dstMimeType) {
					case PNG:
					case JPG:
					case BMP:
						if (srcMimeType == dstMimeType) {
							dstType = inImage.getType();
						}
						break;
				}
	
				//
				// find out the dst size
				//
				rp = computeResizeDim(new Dim(srcWidth, srcHeight), 
						new Dim(dstWidth, dstHeight), keepAspectRatio, upTo);

				// adjust the dst dim
				boolean doCopy = false;
				if (upTo) {
					if (rp.getDim().getWidth() >= srcWidth || rp.getDim().getHeight() >= srcHeight) {
						// do nothing but copy
						rp.getDim().set(srcWidth, srcHeight);
					}
					
					if (rp.getRatio().isOne() && dstMimeType == srcMimeType) {
						// simply copy the source stream
						doCopy = true;
					}
				}				
				
				if (doCopy) {
					//in.reset();
					//IOUtils.copy(in, out);
					ret.copyRequired = true;
				}
				else {
					
					//
					// convert color model as necessary if it's indexed
					//
					BufferedImage inWork;
					if (dstType <= 0) {//inImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
						switch (dstMimeType) {
							case PNG:
								dstType = BufferedImage.TYPE_4BYTE_ABGR;
								break;

							case BMP:
							case JPG:
								dstType = BufferedImage.TYPE_3BYTE_BGR;
								break;
								
								
						}

						inWork = new BufferedImage(srcWidth, srcHeight, dstType);
						ColorConvertOp ccop = new ColorConvertOp(
								inImage.getColorModel().getColorSpace(),
								ColorSpace.getInstance(ColorSpace.CS_sRGB),
								null);

						ccop.filter(inImage, inWork);


					}
					else {
						inWork = inImage;
					}





					// create output buffered image, no colormodel
					BufferedImage outImage = new BufferedImage(rp.getDim().getWidth(), rp.getDim().getHeight(), dstType);
					resize(inWork, outImage);

					// write to output stream
					iw = getImageWriter(dstMimeType);
					ios = ImageIO.createImageOutputStream(out);
					iw.setOutput(ios);
					iw.write(outImage);
					ios.flush();
				}
			}
		}
		catch (ResizeImageFailureException e) {
			throw e;
		}
		catch (Throwable t) {
			LogUtil.errorAndThrow(logger, t,
					new ResizeImageFailureException(t, "Unable to resize image"));
		}
		finally {
			CleanupUtil.release(iis, ir, ios, iw);
		}
		
		ret.dim = rp.getDim();
		return ret;
		
	}
	
	/**
	 * Resize gif
	 * 
	 * Assume that each frame has same width?
	 * no as GIF may have different dim across different frames
	 * 
	 * For simplicity, only check the first frame's size
	 * if it is smaller than the max, copy
	 * 
	 * @param ir
	 * @param out
	 * @param dstDim
	 * @param keepAspectRatio
	 * @param upTo
	 * @param dstQuality
	 * @return 
	 */
	private static ResizeParam resizeGif(
		InputStream			in,
		OutputStream		out, 
		ImageReader			ir, 
		Dim					dstDim,
		boolean				keepAspectRatio, 
		boolean				upTo
	) throws IOException, ResizeImageFailureException {
		
		ResizeParam dstResizeParam = null;

		BufferedImage biIn, biOut;

		// output stream
		ImageOutputStream ios = null;//ImageIO.createImageOutputStream(out);
		ImageWriter iw = null;//getImageWriter(MimeType.GIF);
		

		// check number of frames
		int num = ir.getNumImages(true);

		// get default write param
		ImageWriteParam writeParam = null;//iw.getDefaultWriteParam();
		GifMetadata gifMetadata;
		IIOMetadata dstMetadata;

		// find maximum size of all the frames
		Dim dstDim1 = new Dim();
		Dim srcDim;

		boolean doCopy = false;
		for (int i = 0; i < num; ++ i) {

			// get the first one
			biIn = ir.read(i);

			if (i == 0) {

				// prepare biOut
				srcDim = new Dim(biIn.getWidth(), biIn.getHeight());
				dstResizeParam = computeResizeDim(srcDim, dstDim, keepAspectRatio, upTo);
				
				if (dstResizeParam.getRatio().isOne()) {
					doCopy = true;
					break;
				}
				
				
				dstDim1.set(dstResizeParam.getDim());

				// do preparation
				writeParam = iw.getDefaultWriteParam();
				ios = ImageIO.createImageOutputStream(out);
				iw = getImageWriter(MimeType.GIF);
				iw.setOutput(ios);
				iw.prepareWriteSequence(null);
			}
			else {
				// need to calculate
				dstDim1.set(dstResizeParam.getRatio().getRx() * biIn.getWidth(), dstResizeParam.getRatio().getRy() * biIn.getHeight());
			}

			//System.out.println(String.format("minX = %1$d, minY = %2$d", biIn.getMinX(), biIn.getMinY()));

			biOut = createBufferedImage(dstDim1.getWidth(), dstDim1.getHeight(), biIn);
			resize(biIn, biOut);

			// get current metadata
			gifMetadata = getGifMetadata(ir, i);

			// re-calculate the position and size
			gifMetadata.setImageWidth(dstDim1.getWidth());
			gifMetadata.setImageHeight(dstDim1.getHeight());
			gifMetadata.setImageLeftPosition((int)Math.round(dstResizeParam.getRatio().getRx() * gifMetadata.getImageLeftPosition()));
			gifMetadata.setImageTopPosition((int)Math.round(dstResizeParam.getRatio().getRy() * gifMetadata.getImageTopPosition()));

			// generate the IIOMetadata
			dstMetadata = createIIOMetadata(biOut, iw, gifMetadata);

			iw.writeToSequence(new IIOImage(biOut, null, dstMetadata), writeParam);
		}
		
		if (!doCopy) {
			iw.endWriteSequence();

			ios.flush();
		}
		else {
			in.reset();
			IOUtils.copy(in, out);
		}

		
		return dstResizeParam;
	}
	
	/**
	 * Create buffered image using reference buffered image, i.e.
	 * the created one has the passed-in dimension and color model/type
	 * @param refbi
	 * @return 
	 */
	public static BufferedImage createBufferedImage(int width, int height, BufferedImage biRef) {
		BufferedImage biRet;
		if (biRef.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
			biRet = new BufferedImage(width, height, biRef.getType(), (IndexColorModel)biRef.getColorModel());
		}
		else {
			int type = biRef.getType();
			if (type == 0) {
				type = BufferedImage.TYPE_4BYTE_ABGR;
			}
			biRet = new BufferedImage(width, height, type);
		}
		
		return biRet;
		
	}
	
	/**
	 * 
	 * @param srcWidth
	 * @param srcHeight
	 * @param keepAspectRatio
	 * @param upTo
	 * @return 
	 */
	public static ResizeParam computeResizeDim(Dim srcDim, Dim dstDim, boolean keepAspectRatio, boolean upTo) {
		//
		// find out the dst size
		//
		Dim retDim = new Dim();
		double ratioX, ratioY;

		if (keepAspectRatio) {
			double srcyx = srcDim.getHeight() / srcDim.getWidth();
			double dstyx = dstDim.getHeight() / dstDim.getWidth();
			if (dstyx > srcyx) {
				ratioX = ratioY = (double)dstDim.getWidth() / srcDim.getWidth();
				retDim.setWidth(dstDim.getWidth());
				retDim.setHeight((int)Math.round(ratioX * srcDim.getHeight()));
			}
			else {
				ratioX = ratioY = (double)dstDim.getHeight() / srcDim.getHeight();
				retDim.setHeight(dstDim.getHeight());
				retDim.setWidth((int)Math.round(ratioX * srcDim.getWidth()));
			}
		}
		else {
			ratioX = (double)dstDim.getWidth() / srcDim.getWidth();
			ratioY = (double)dstDim.getHeight() / srcDim.getHeight();

			retDim.set(dstDim);
		}
		
		if (upTo) {
			if (retDim.getWidth() >= srcDim.getWidth() || retDim.getHeight() >= srcDim.getHeight()) {
				retDim.set(srcDim);
				ratioX = ratioY = 1;
			}
		}
		
		return new ResizeParam(retDim, new Ratio(ratioX, ratioY));
	}
	
	
	public static BufferedImage resize(
		BufferedImage			biIn, 
		int						dstWidth, 
		int						dstHeight, 
		boolean					keepAspectRatio,
		boolean					upTo
	) throws ResizeImageFailureException {
		Dim srcDim = new Dim(biIn.getWidth(), biIn.getHeight());
		Dim dstDim = new Dim(dstWidth, dstHeight);
		
		ResizeParam rp = computeResizeDim(srcDim, dstDim, keepAspectRatio, upTo);
		BufferedImage biOut = createBufferedImage(rp.getDim().getWidth(), rp.getDim().getHeight(), biIn);
		resize(biIn, biOut);
		
		return biOut;
	}	
	
	
	
	/**
	 * 
	 * dst image type is always same as the source image type
	 * @param biIn
	 * @param biOut
	 * @param dstWidth
	 * @param dstHeight
	 * @param keepAspectRatio
	 * @param upTo
	 * @throws ResizeImageFailureException 
	 */
	public static void resize(
		BufferedImage			biIn, 
		BufferedImage			biOut
	) throws ResizeImageFailureException {
		
		try {
			
			Dim srcDim = new Dim(biIn.getWidth(), biIn.getHeight());
			Dim dstDim = new Dim(biOut.getWidth(), biOut.getHeight());
			
			if (srcDim.equals(dstDim)) {
				biOut.setData(biIn.getData());
			}
			else {
				double ratioX = (double)dstDim.getWidth() / srcDim.getWidth();
				double ratioY = (double)dstDim.getHeight() / srcDim.getHeight();
				int transAlgo;
				
				if (biIn.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
					transAlgo = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
				}
				else {
					transAlgo = AffineTransformOp.TYPE_BICUBIC;
				}

				AffineTransform tx = new AffineTransform();


				tx.scale(ratioX, ratioY);

				AffineTransformOp txop = new AffineTransformOp(tx, transAlgo);
				txop.filter(biIn, biOut);
			}
		}
		catch (Throwable t) {
			LogUtil.errorAndThrow(logger, t,
					new ResizeImageFailureException(t, "Unable to resize image"));
		}
	}

	
	
	
	///////////////////////////////////////////////////////////////////////
	///////////// Testing codes ///////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////
	/*
	private void test1() {
		ImageInfo iiDefault = new ImageInfo();
		ImageInfo iiIn = new ImageInfo("C:\\hud\\tmp\\test1.gif");
		MagickImage mi = new MagickImage(iiIn);
		
		int dstWidth = 50;
		int dstHeight = 50;
		if (mi.isAnimatedImage()) {
			MagickImage[] frames = mi.breakFrames();
			MagickImage[] dstFrames = new MagickImage[frames.length];
			
			for (int i = 0; i < frames.length; ++ i) {
				dstFrames[i] = frames[i].scaleImage(dstWidth, dstHeight);
			}
			
			MagickImage miOut = new MagickImage(dstFrames);
			miOut.setFileName("C:\\hud\\tmp\\test1_out.gif");
			
			miOut.writeImage(iiDefault);
		}
				
	}
	*/
	
	
	
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	////////////////////////////// GIF reader/writer //////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	public static enum GifMetadataAttr {
		GraphicControlExtension
	,	CommentExtensions
	,	CommentExtension
	,	ApplicationExtensions
	,	ApplicationExtension
	,	transparentColorIndex
	,	delayTime
	,	disposalMethod
	,	userInputFlag
	,	transparentColorFlag
	,	applicationID
	,	authenticationCode
	
	,	ImageDescriptor
	,	imageLeftPosition
	,	imageTopPosition
	,	imageWidth
	,	imageHeight
	}
	
	public static class GifMetadata {
		public static final String DEFAULT_COMMENT_EXTENSION = "Created by ME";
		public static final String DEFAULT_APPLICATION_ID = "NETSCAPE";
		public static final String DEFAULT_AUTHENTICATION_CODE = "2.0";
		
		private String			disposalMethod = "doNotDispose";	//"restoreToBackgroundColor" | "restoreToPrevious"
		private int				transparentColorIndex = 0;	//0 - 255, valid if transparentColorFlag == true
		private String			delayTime = "10";		// unit: 1/100 s, may use "x" as devider
		private boolean			userInputFlag = false;
		private boolean			transparentColorFlag = true; // 
		private String			commentExtension = DEFAULT_COMMENT_EXTENSION;
		private String			applicationID = DEFAULT_APPLICATION_ID;
		private String			authenticationCode = DEFAULT_AUTHENTICATION_CODE;
		private int				imageLeftPosition = 0;
		private int				imageTopPosition = 0;
		private int				imageWidth = 0;
		private int				imageHeight = 0;
		
		// loop = 0 if loop continuously, otherwise 1
		private byte[]			loopModel = new byte[3]; // new byte[]{ 0x1, (byte) (loop & 0xFF), (byte)((loop >> 8) & 0xFF)};
		
		
		public GifMetadata() {
			// initialize the loopModel with default value
			loopModel[0] = 0x1;
			loopModel[1] = 0;
			loopModel[2] = 0;
		}

		public String getDelayTime() {
			return delayTime;
		}

		public void setDelayTime(String delayTime) {
			this.delayTime = delayTime;
		}

		public String getDisposalMethod() {
			return disposalMethod;
		}

		public void setDisposalMethod(String disposalMethod) {
			this.disposalMethod = disposalMethod;
		}

		public byte[] getLoopModel() {
			return loopModel;
		}

		public void setLoopModel(byte[] loopModel) {
			this.loopModel = loopModel;
		}

		public boolean isTransparentColorFlag() {
			return transparentColorFlag;
		}

		public void setTransparentColorFlag(boolean transparentColorFlag) {
			this.transparentColorFlag = transparentColorFlag;
		}

		public int getTransparentColorIndex() {
			return transparentColorIndex;
		}

		public void setTransparentColorIndex(int transparentColorIndex) {
			this.transparentColorIndex = transparentColorIndex;
		}

		public boolean isUserInputFlag() {
			return userInputFlag;
		}

		public void setUserInputFlag(boolean userInputFlag) {
			this.userInputFlag = userInputFlag;
		}

		public String getCommentExtension() {
			return commentExtension;
		}

		public void setCommentExtension(String commentExtension) {
			this.commentExtension = commentExtension;
		}

		public String getApplicationID() {
			return applicationID;
		}

		public void setApplicationID(String applicationID) {
			this.applicationID = applicationID;
		}

		public String getAuthenticationCode() {
			return authenticationCode;
		}

		public void setAuthenticationCode(String authenticationCode) {
			this.authenticationCode = authenticationCode;
		}

		public int getImageLeftPosition() {
			return imageLeftPosition;
		}

		public void setImageLeftPosition(int imageLeftPosition) {
			this.imageLeftPosition = imageLeftPosition;
		}

		public int getImageTopPosition() {
			return imageTopPosition;
		}

		public void setImageTopPosition(int imageTopPosition) {
			this.imageTopPosition = imageTopPosition;
		}

		public int getImageHeight() {
			return imageHeight;
		}

		public void setImageHeight(int imageHeight) {
			this.imageHeight = imageHeight;
		}

		public int getImageWidth() {
			return imageWidth;
		}

		public void setImageWidth(int imageWidth) {
			this.imageWidth = imageWidth;
		}
		
	}
	
	public static IIOMetadataNode getIIONode(IIOMetadataNode root, String nodeName, boolean create) {
		
		IIOMetadataNode retNode = null;
		
		int nNodes = root.getLength();
		for (int i = 0; i < nNodes; i++) {
			if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(root.item(i).getNodeName(), nodeName)) {
				retNode = ((IIOMetadataNode) root.item(i));
				break;
			}
		}
		
		if (retNode == null && create) {
		
			// if no node exists
			retNode = new IIOMetadataNode(nodeName);
			root.appendChild(retNode);
		}
		
		return retNode;
		
	}
	
	public static GifMetadata getGifMetadata(ImageReader ir, int index) throws IOException {
		return getGifMetadata(ir.getImageMetadata(index));
	}
	
	public static GifMetadata getGifMetadata(IIOMetadata iioMetadata) {
		GifMetadata gifMetadata = new GifMetadata();
		
		String metadataFormatName = iioMetadata.getNativeMetadataFormatName();
		IIOMetadataNode root = (IIOMetadataNode)iioMetadata.getAsTree(metadataFormatName);
		
		String val;

		// get position
		IIOMetadataNode idNode = getIIONode(root, GifMetadataAttr.ImageDescriptor.toString(), false);
		if (idNode != null) {
			val = idNode.getAttribute(GifMetadataAttr.imageLeftPosition.toString());
			if (val != null) {
				gifMetadata.setImageLeftPosition(Integer.valueOf(val));
			}
			
			val = idNode.getAttribute(GifMetadataAttr.imageTopPosition.toString());
			if (val != null) {
				gifMetadata.setImageTopPosition(Integer.valueOf(val));
			}

			val = idNode.getAttribute(GifMetadataAttr.imageWidth.toString());
			if (val != null) {
				gifMetadata.setImageWidth(Integer.valueOf(val));
			}

			val = idNode.getAttribute(GifMetadataAttr.imageHeight.toString());
			if (val != null) {
				gifMetadata.setImageHeight(Integer.valueOf(val));
			}
		}
		//
		
		IIOMetadataNode gceNode = getIIONode(root, GifMetadataAttr.GraphicControlExtension.toString(), false);
		if (gceNode == null) {
			return null;
		}
		
		
		val = gceNode.getAttribute(GifMetadataAttr.disposalMethod.toString());
		if (val != null) {
			gifMetadata.setDisposalMethod(val);
		}
		
		val = gceNode.getAttribute(GifMetadataAttr.userInputFlag.toString());
		if (val != null) {
			gifMetadata.setUserInputFlag(Boolean.valueOf(val));
		}		

		val = gceNode.getAttribute(GifMetadataAttr.transparentColorFlag.toString());
		if (val != null) {
			gifMetadata.setTransparentColorFlag(Boolean.valueOf(val));
		}
		
		val = gceNode.getAttribute(GifMetadataAttr.delayTime.toString());
		if (val != null) {
			gifMetadata.setDelayTime(val);
		}		
		
		val = gceNode.getAttribute(GifMetadataAttr.transparentColorIndex.toString());
		if (val != null) {
			gifMetadata.setTransparentColorIndex(Integer.valueOf(val));
		}
		
		IIOMetadataNode commentsNode = getIIONode(root, GifMetadataAttr.CommentExtensions.toString(), false);
		if (commentsNode != null) {
			val = commentsNode.getAttribute(GifMetadataAttr.CommentExtension.toString());
			if (val != null) {
				gifMetadata.setCommentExtension(val);
			}
		}
		
		IIOMetadataNode appExtsNode = getIIONode(root, GifMetadataAttr.ApplicationExtensions.toString(), false);
		if (appExtsNode != null) {
			IIOMetadataNode appExtNode = getIIONode(appExtsNode, GifMetadataAttr.ApplicationExtension.toString(), false);
			if (appExtNode != null) {
				val = appExtNode.getAttribute(GifMetadataAttr.applicationID.toString());
				if (val != null) {
					gifMetadata.setApplicationID(val);
				}
				
				val = appExtNode.getAttribute(GifMetadataAttr.authenticationCode.toString());
				if (val != null) {
					gifMetadata.setAuthenticationCode(val);
				}
				
				byte[] loopModel = (byte[])appExtNode.getUserObject();
				if (loopModel != null) {
					gifMetadata.setLoopModel(loopModel);
				}
			}
		}
		
		
		return gifMetadata;
	}

	/**
	 * 
	 */
	public static IIOMetadata createIIOMetadata(BufferedImage bi, ImageWriter iw, GifMetadata gifMetadata) 
	throws IIOInvalidTreeException {
		ImageWriteParam writeParam = iw.getDefaultWriteParam();
		ImageTypeSpecifier imageTypeSpecifier = new ImageTypeSpecifier(bi);
		IIOMetadata iioMetadata = iw.getDefaultImageMetadata(imageTypeSpecifier, writeParam);
		
		return fillIIOMetadata(iioMetadata, gifMetadata);
	}
	
	/**
	 * 
	 * @param iioMetadata
	 * @param gifMetadata
	 * @return 
	 */
	public static IIOMetadata fillIIOMetadata(IIOMetadata iioMetadata, GifMetadata gifMetadata) throws IIOInvalidTreeException {
		
		String metadataFormatName = iioMetadata.getNativeMetadataFormatName();
		IIOMetadataNode root = (IIOMetadataNode)iioMetadata.getAsTree(metadataFormatName);
		
		// image position
		IIOMetadataNode idNode = getIIONode(root, GifMetadataAttr.ImageDescriptor.toString(), true);
		idNode.setAttribute(GifMetadataAttr.imageLeftPosition.toString(), Integer.toString(gifMetadata.getImageLeftPosition()));
		idNode.setAttribute(GifMetadataAttr.imageTopPosition.toString(), Integer.toString(gifMetadata.getImageTopPosition()));
		idNode.setAttribute(GifMetadataAttr.imageWidth.toString(), Integer.toString(gifMetadata.getImageWidth()));
		idNode.setAttribute(GifMetadataAttr.imageHeight.toString(), Integer.toString(gifMetadata.getImageHeight()));
		
		IIOMetadataNode gceNode = getIIONode(root, GifMetadataAttr.GraphicControlExtension.toString(), false);
		if (gceNode == null) {
			return null;
		}
		
		gceNode.setAttribute(GifMetadataAttr.disposalMethod.toString(), gifMetadata.getDisposalMethod());
		gceNode.setAttribute(GifMetadataAttr.userInputFlag.toString(), Boolean.toString(gifMetadata.isUserInputFlag()));
		gceNode.setAttribute(GifMetadataAttr.transparentColorFlag.toString(), Boolean.toString(gifMetadata.isTransparentColorFlag()));
		gceNode.setAttribute(GifMetadataAttr.delayTime.toString(), gifMetadata.getDelayTime());
		gceNode.setAttribute(GifMetadataAttr.transparentColorIndex.toString(), Integer.toString(gifMetadata.getTransparentColorIndex()));
		
		IIOMetadataNode commentsNode = getIIONode(root, GifMetadataAttr.CommentExtensions.toString(), true);
		commentsNode.setAttribute(GifMetadataAttr.CommentExtension.toString(), gifMetadata.getCommentExtension());
		
		IIOMetadataNode appExtsNode = getIIONode(root, GifMetadataAttr.ApplicationExtensions.toString(), true);
		IIOMetadataNode appExtNode = getIIONode(appExtsNode, GifMetadataAttr.ApplicationExtension.toString(), true);
		
		appExtNode.setAttribute(GifMetadataAttr.applicationID.toString(), gifMetadata.getApplicationID());
		appExtNode.setAttribute(GifMetadataAttr.authenticationCode.toString(), gifMetadata.getAuthenticationCode());
		appExtNode.setUserObject(gifMetadata.getLoopModel());

		
		
		iioMetadata.setFromTree(metadataFormatName, root);
		
		return iioMetadata;
	}
}
