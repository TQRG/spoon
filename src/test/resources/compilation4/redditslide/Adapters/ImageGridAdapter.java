package me.ccrama.redditslide.Adapters;
import me.ccrama.redditslide.Reddit;
import java.util.ArrayList;
import java.util.List;
import me.ccrama.redditslide.ImgurAlbum.Image;
import me.ccrama.redditslide.Tumblr.Photo;
/**
 * Created by carlo_000 on 3/20/2016.
 */
public class ImageGridAdapter extends android.widget.BaseAdapter {
    private android.content.Context mContext;

    private java.util.List<java.lang.String> jsons;

    public static final com.nostra13.universalimageloader.core.DisplayImageOptions options = new com.nostra13.universalimageloader.core.DisplayImageOptions.Builder().cacheOnDisk(true).resetViewBeforeLoading(true).bitmapConfig(android.graphics.Bitmap.Config.RGB_565).imageScaleType(com.nostra13.universalimageloader.core.assist.ImageScaleType.EXACTLY).cacheInMemory(false).displayer(new com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer(250)).build();

    public ImageGridAdapter(android.content.Context c, java.util.List<me.ccrama.redditslide.ImgurAlbum.Image> imgurAlbum) {
        mContext = c;
        jsons = new java.util.ArrayList<>();
        for (me.ccrama.redditslide.ImgurAlbum.Image i : imgurAlbum) {
            jsons.add(i.getThumbnailUrl());
        }
    }

    public ImageGridAdapter(android.content.Context c, java.util.List<me.ccrama.redditslide.Tumblr.Photo> tumblrAlbum, boolean tumblr) {
        mContext = c;
        jsons = new java.util.ArrayList<>();
        for (me.ccrama.redditslide.Tumblr.Photo i : tumblrAlbum) {
            jsons.add((i.getAltSizes() != null) && (!i.getAltSizes().isEmpty()) ? i.getAltSizes().get(0).getUrl() : i.getOriginalSize().getUrl());
        }
    }

    public int getCount() {
        return jsons.size();
    }

    public java.lang.String getItem(int position) {
        return jsons.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
        android.widget.ImageView imageView;
        android.widget.GridView grid = ((android.widget.GridView) (parent));
        int size = grid.getColumnWidth();
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new android.widget.ImageView(mContext);
        } else {
            imageView = ((android.widget.ImageView) (convertView));
        }
        imageView.setLayoutParams(new android.widget.AbsListView.LayoutParams(size, size));
        imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        ((me.ccrama.redditslide.Reddit) (mContext.getApplicationContext())).getImageLoader().displayImage(getItem(position), imageView, me.ccrama.redditslide.Adapters.ImageGridAdapter.options);
        return imageView;
    }
}