package com.verizon.mms.ui.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.widget.EmojiGridView;
import com.verizon.mms.util.EmojiParser;

public class EmojiPagerAdapter extends PagerAdapter {
	private final Context context;
	private LayoutInflater inflater;
	private int[] icons = EmojiParser.DEFAULT_EMOJI_RES_IDS;
	private EmojiParser parser = null;
	private OnEmojiSelectedListener emojiListener;
	private final int iconDimension;
	public interface OnEmojiSelectedListener {
		public void onEmojiSelected(CharSequence emoji);
	}
	
	public EmojiPagerAdapter(Context ctx, ViewPager viewPager, OnEmojiSelectedListener listener) {
		this.context = ctx;
		inflater = LayoutInflater.from(ctx);
		parser = EmojiParser.getInstance();
		emojiListener = listener;
		iconDimension = ctx.getResources().getDimensionPixelSize(R.dimen.emoji_icon_width);
	}

	@Override
	public int getCount() {
		return EmojiParser.NUM_OF_SECTIONS;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		View view = inflater.inflate(R.layout.emoji_view, null);
		int[] icons = getEmojis(position);
		final EmojiGridView gridView = (EmojiGridView) view.findViewById(R.id.emoji_grid_view);
        final EmojiImageAdapter emojiAdapter = new EmojiImageAdapter(context, icons, position);
		
        gridView.setGridMargin();
        gridView.setAdapter(new EmojiImageAdapter(context, icons, position));
        gridView.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int section = emojiAdapter.getEmojiSection();
        		CharSequence emoji = parser.addEmojiSpans(getEmojiText(section, position), false);
                emojiListener.onEmojiSelected(emoji);
            }
		});
		(container).addView(view, 0);
		
		return view;
	}
	
	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	@Override
	public void finishUpdate(View arg0) {
	}
	
	private int[] getEmojis(int position) {
    	int start = -1;
    	int end = -1;
    	int [] emojis = null;
        switch (position) {
        case 0:
            start = EmojiParser.SECTION_ONE_START_INDEX;
            end = EmojiParser.SECTION_TWO_START_INDEX;
            break;
        case 1:
        	start = EmojiParser.SECTION_TWO_START_INDEX;
            end = EmojiParser.SECTION_THREE_START_INDEX;
            break;
        case 2:
            start = EmojiParser.SECTION_THREE_START_INDEX;
            end = EmojiParser.SECTION_FOUR_START_INDEX;
            break;
        case 3:
            start = EmojiParser.SECTION_FOUR_START_INDEX;
            end = EmojiParser.SECTION_FIVE_START_INDEX;
            break;
        case 4:
        	start = EmojiParser.SECTION_FIVE_START_INDEX;
            end = EmojiParser.EMOJIS_LAST_INDEX;
            break;
        }
        
        if (start != -1) {
        	emojis = new int[end - start];
        	for (int i = start, j = 0; i < end; i++, j++) {
                emojis[j] = icons[i];
            }
        }
        return emojis;
    }
	
	public CharSequence getEmojiText(int section, int position) {
        CharSequence emoji = null;
        switch (section) {
        case 0:
            position = EmojiParser.SECTION_ONE_START_INDEX + position;
            emoji = EmojiParser.allEmojiTexts[position];
            break;
        case 1:
            position = EmojiParser.SECTION_TWO_START_INDEX + position;
            emoji = EmojiParser.allEmojiTexts[position];
            break;
        case 2:
            position = EmojiParser.SECTION_THREE_START_INDEX + position;
            emoji = EmojiParser.allEmojiTexts[position];
            break;
        case 3:
            position = EmojiParser.SECTION_FOUR_START_INDEX + position;
            emoji = EmojiParser.allEmojiTexts[position];
            break;
        case 4:
            position = EmojiParser.SECTION_FIVE_START_INDEX + position;
            emoji = EmojiParser.allEmojiTexts[position];
            break;
        }
        return emoji;
    }
	
	public class EmojiImageAdapter extends BaseAdapter {
	    private Context mContext;
	    private int[] resIds;
	    private final int section;
	    public EmojiImageAdapter(Context c, int[] resIds, int position) {
	        mContext = c;
	        this.resIds = resIds;
	        this.section = position;
	    }

	    public int getCount() {
	    	return resIds.length;
	    }

	    public Object getItem(int position) {
	        return null;
	    }

	    public long getItemId(int position) {
	        return 0;
	    }

	    // create a new ImageView for each item referenced by the Adapter
	    public View getView(int position, View convertView, ViewGroup parent) {
	        ImageView imageView;
	        if (convertView == null) {  // if it's not recycled, initialize some attributes
	            imageView = new ImageView(mContext);
	            imageView.setLayoutParams(new GridView.LayoutParams(iconDimension, iconDimension));
	            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
	            imageView.setPadding(2, 2, 2, 2);
	        } else {
	            imageView = (ImageView) convertView;
	        }

	        imageView.setImageResource(resIds[position]);
	        return imageView;
	    }
	    
	    public int getEmojiSection() {
	    	return section;
	    }
	}
}