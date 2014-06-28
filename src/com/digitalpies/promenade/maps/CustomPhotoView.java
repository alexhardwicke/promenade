package com.digitalpies.promenade.maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

import com.digitalpies.promenade.database.Photo;
import com.google.android.maps.OverlayItem;
import com.digitalpies.promenade.R;

/**
 * A custom implementation of CustomOverlayView that returns a custom view containing an ImageView
 * for each photo.<br>
 * <br>
 * The ImageView is also sent to a ViewPager with a PhotoPagerAdapter that sets the ImageView to
 * have the appropriate image. Until the ViewPager has set the image, the ImageView's image consists
 * of two RotateDrawables, rotated by a custom AsyncTask - AnimationTask.<br>
 * <br>
 * Each image in the PhotoPagerAdapter is retrieved via an AsyncTask to prevent lag as the photo is
 * loaded.
 * 
 * @author Alex Hardwicke
 */
public class CustomPhotoView extends CustomOverlayView<OverlayItem>
{
	private AnimationTask animTask = null;
	private ArrayList<Photo> photos;
	private LayerDrawable layerDrawable;
	private RotateDrawable outerSpinner;
	private RotateDrawable innerSpinner;
	private PhotoPagerAdapter photoAdapter;

	public int bitmapInstancesRunning = 0;

	/**
	 * The constructor. Runs the super-constructor and retrieves the photos.<br>
	 * <br>
	 * Also retrieves the animated "loading" layerDrawable from resources and the two images
	 * inside the layerDrawable.
	 * 
	 * @param context				The application context
	 * @param balloonBottomOffset	How high above the overlay item the balloon should be
	 * @param activity				The application activity
	 */
	public CustomPhotoView(Context context, int balloonBottomOffset, CustomMapActivity activity)
	{
		super(context, balloonBottomOffset, activity);
		this.photos = activity.getPhotos();

		this.layerDrawable = (LayerDrawable) getResources().getDrawable(R.drawable.animated_loading_spinner);
		this.outerSpinner = (RotateDrawable) this.layerDrawable.getDrawable(0);
		this.innerSpinner = (RotateDrawable) this.layerDrawable.getDrawable(1);
	}

	/**
	 * Run when the view is being set up. Inflates the balloon_photo_pager layout in the super method,
	 * sets up a PhotoPager adapter and sets the photoPager's off-screen page limit to 5.
	 */
	@Override
	protected void setupView(Context context, final ViewGroup parent)
	{
		super.setupView(context, parent, R.layout.balloon_photo_pager);
		
		this.photoAdapter = new PhotoPagerAdapter();
		this.pager.setOffscreenPageLimit(5);
	}

	/**
	 * Run when the balloon data has been sent to the class. Passes the adapter to the view pager.<br>
	 * <br>
	 * If the size of the photo ArrayList is greater than two and less than 22, it gives the
	 * indicator a reference to the view pager and provides the CustomOnPageChangeListener to the
	 * indicator. Otherwise, it hides the indicator, changes the photopager's top padding to 25,
	 * and provides the CustomOnPageChangeListener to the view pager.
	 */
	@Override
	protected void setBalloonData(OverlayItem item, ViewGroup parent)
	{
		super.setBalloonData(this.photoAdapter, this.photos.size(), item);

		// If there's only one photo or more than 21, the indicator is missing and
		// the pager padding should be changed
		if (this.photos.size() < 1 || this.photos.size() > 21)
		{
			this.pager.setPadding(this.pager.getPaddingLeft(), 25, this.pager.getPaddingRight(),
					this.pager.getPaddingBottom());
		}
	}

	/**
	 * Reads a Bitmap using the provided uri, scaled down for memory safety.<br>
	 * <br>
	 * Returns null if it cannot load the bitmap. If this happens, the calling method generates
	 * a TextView instead.<br>
	 * 
	 * @param uri	The uri of the photo to load
	 * 
	 * @return		Null, or the loaded Bitmap at 1/8 scale
	 */
	public Bitmap readBitmap(Uri uri)
	{
		Bitmap bitmap = null;

		// Create BitmapFactory.Options to load the image at 1/8 of the size of the full image.
		// 1/8 was chosen because powers of 2 are faster, 1/4 size sometimes uses too much memory,
		// and 1/16 is too low res
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 8;
		AssetFileDescriptor assetFileDescriptor = null;
		try
		{
			// Get an assetFileDescriptor for the file
			assetFileDescriptor = this.activity.getContentResolver().openAssetFileDescriptor(uri, "r");
		}
		catch (FileNotFoundException e)
		{
			// File not found - return null, so that an error is shown instead
			return null;
		}
		finally
		{
			try
			{
				// If the assetfiledescriptor is null (safety check), return null so an error is shown instead
				if (assetFileDescriptor == null) return null;

				// Set the bitmap and close the fileDescriptor
				bitmap = BitmapFactory.decodeFileDescriptor(assetFileDescriptor.getFileDescriptor(), null, options);
				assetFileDescriptor.close();
			}
			catch (IOException e)
			{
				// Error decoding the bitmap, return null so an error is shown instead
				return null;
			}
		}
		return bitmap;
	}

	/**
	 * A custom PagerAdapter that displays the correct photo or a TextView telling the user that
	 * the photo file is missing.<br>
	 * 
	 * @author Alex Hardwicke
	 */
	private class PhotoPagerAdapter extends CustomPagerAdapter
	{
		@Override
		public int getCount()
		{
			return CustomPhotoView.this.photos.size();
		}

		@Override
		public Object instantiateItem(View collection, int pagerPosition)
		{
			// Create an ImageView
			ImageView imageView = new ImageView(CustomPhotoView.this.activity);

			// Prevent the image from scaling, and set the image to the layerDrawable created in
			// the constructor
			imageView.setScaleType(ScaleType.CENTER);
			imageView.setImageDrawable(CustomPhotoView.this.layerDrawable);

			// If the animTask is null (so this is the first item for this adapter), start it, set
			// the animtask in the activity, and execute the task alongside other tasks (not serial)
			if (CustomPhotoView.this.animTask == null)
			{
				CustomPhotoView.this.animTask = new AnimationTask(imageView);
				CustomPhotoView.this.activity.setAnimTask(CustomPhotoView.this.animTask);
				CustomPhotoView.this.animTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);
			}
			else
				CustomPhotoView.this.animTask.addImageView(imageView);

			// Create a bitmapworker task to decode the image for this position. These are set to serial
			// so that the images are processed in order. A reference to each task is passed to the
			// activity, so that the tasks can be cancelled if required. Note that the individual tasks
			// do not cancel when in progress, but this prevents all pending tasks from running.
			BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(imageView);
			bitmapWorkerTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, pagerPosition);
			CustomPhotoView.this.activity.addBitmapWorkerTask(bitmapWorkerTask);

			// Set the view to the photo (or TextView if the photo can't be found)
			((ViewPager) collection).addView(imageView, 0);

			return imageView;
		}
	}

	/**
	 * An implementation of AsyncTask responsible for animating the "loading" icon on the
	 * image views until photos have loaded. Has a reference to an ArrayList of WeakReferences,
	 * each one being a weak reference to an ImageView and simply updates the "level" value
	 * of all the images by 100 every 30 milliseconds.
	 * 
	 * @author Alex Hardwicke
	 */
	private class AnimationTask extends AsyncTask<Integer, Integer, Integer>
	{
		private ArrayList<WeakReference<ImageView>> imageViews = new ArrayList<WeakReference<ImageView>>();

		public AnimationTask(ImageView imageView)
		{
			this.imageViews.add(new WeakReference<ImageView>(imageView));
		}

		/**
		 * Adds an extra ImageView to the ArrayList.
		 * @param view
		 */
		public void addImageView(ImageView view)
		{
			this.imageViews.add(new WeakReference<ImageView>(view));
		}

		@Override
		protected Integer doInBackground(Integer... arg0)
		{
			int i = 0;
			// If the image isn't set and the task hasn't been cancelled
			while (!isCancelled())
			{
				// Publish the current progress, increase it by 100, and wait 30 ms.
				publishProgress(i);
				i += 100;
				try
				{
					Thread.sleep(30);
				}
				catch (InterruptedException e)
				{
					break;
				}

				// If no bitmap instances are running, all "loading" icons must be covered.
				// Stop running!
				if (CustomPhotoView.this.bitmapInstancesRunning == 0) break;
			}
			return 0;
		}

		@Override
		protected void onProgressUpdate(Integer... args)
		{
			// Retrieve the level value
			int i = args[0];

			// Spin the images
			CustomPhotoView.this.outerSpinner.setLevel(i);
			CustomPhotoView.this.innerSpinner.setLevel(i);

			// Invalidate all the imageviews (so that they're updated)
			for (WeakReference<ImageView> reference : this.imageViews)
			{
				if (reference == null) continue;
				ImageView imageView = reference.get();
				if (imageView != null) imageView.invalidate();
			}
		}
	}

	/**
	 * An implementation of AsyncTask that's responsible for retrieving a bitmap from the
	 * SD card.<br>
	 * <br>
	 * Retrieves the pagerPosition, the fileText and URI for the photo matching the
	 * pagerPosition. It then reads the photo from the SD card via the readBitmap method.<br>
	 * If the photo isn't null, it tries to get the orientation of the photo. If it
	 * manages, it sets fileFound to true and returns the photo.<br>
	 * If the photo is null, it returns a bitmap created from a drawable sigifying an error.<br>
	 * <br>
	 * Once the task has returned, it makes sure it can get to the ImageView. If it can, it
	 * sets the bitmap to be the image in the ImageView after cancelling the AnimatedTask task.
	 * If the fileFound boolean has been set to true, it sets the ImageView to FIT_CENTER, sets
	 * up the ImageView to have an onClick method opening the appropriate photo, and rotates the
	 * ImageView as appropriate.
	 * 
	 * @author Alex Hardwicke
	 */
	private class BitmapWorkerTask extends AsyncTask<Integer, Integer, Bitmap>
	{
		private final WeakReference<ImageView> viewReference;
		private int pagerPosition = 0;
		private Uri uri;
		private int orientation;
		private boolean fileFound = false;

		/**
		 * Constructor. Only keeps a Weak Reference to the ImageView in case it gets unbound.
		 * 
		 * @param view	The ImageView
		 */
		public BitmapWorkerTask(ImageView view)
		{
			// New instance running
			CustomPhotoView.this.bitmapInstancesRunning++;
			this.viewReference = new WeakReference<ImageView>(view);
		}

		/**
		 * Gets the pagerPosition, and uses that to generate a String and URI from the
		 * fileText. Then reads the uri to a bitmap. If the bitmap isn't null, it
		 * tries to retrieve the orientation. If it succeeds, it sets fileFound to
		 * true and returns the photo. If any stage fails, it returns a bitmap containing
		 * the image_missing drawable.
		 */
		@Override
		protected Bitmap doInBackground(Integer... params)
		{
			this.pagerPosition = params[0];

			String fileText = CustomPhotoView.this.photos.get(this.pagerPosition).getFile();
			this.uri = Uri.fromFile(new File(fileText));

			ExifInterface exifInterface = null;

			// Try to get the photo
			Bitmap photo = readBitmap(this.uri);

			if (photo != null)
			{
				try
				{
					exifInterface = new ExifInterface(fileText);

					// Android loads images rotated awkwardly. Get the EXIF orientation, and set the rotation appropriately.
					this.orientation = Integer.parseInt(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION));
					this.fileFound = true;

					return photo;
				}
				catch (IOException e)
				{
				}
			}
			// Couldn't load the photo - file missing!
			return BitmapFactory.decodeResource(getResources(), R.drawable.image_missing);
		}

		/**
		 * Run after the task has returned bitmap. If the View Reference isn't null and the Image View
		 * can be retrieved, sets the ImageView to have the bitmap, and ends the animTask. If fileFound
		 * is true, sets up the scaleType to FIT_CENTER, sets up an onclick listener, and rotates the
		 * photo as required.
		 */
		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			// One less instance running
			CustomPhotoView.this.bitmapInstancesRunning--;
			if (this.viewReference != null)
			{
				final ImageView imageView = this.viewReference.get();
				if (imageView != null)
				{
					CustomPhotoView.this.animTask = null;

					// Remove the background and add the new bitmap
					imageView.setImageBitmap(bitmap);
					if (this.fileFound)
					{
						imageView.setScaleType(ScaleType.FIT_CENTER);
						imageView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v)
							{
								Intent intent = new Intent();
								intent.setAction(android.content.Intent.ACTION_VIEW);
								intent.setDataAndType(BitmapWorkerTask.this.uri, "image/jpeg");
								try
								{
									CustomPhotoView.this.activity.startActivity(intent);
								}
								catch (ActivityNotFoundException e)
								{
									Toast.makeText(CustomPhotoView.this.activity, R.string.no_gallery_app_installed,
											Toast.LENGTH_SHORT).show();
								}
							}
						});

						switch (this.orientation)
						{
						case ExifInterface.ORIENTATION_ROTATE_90:
							imageView.setRotation(90);
							break;
						case ExifInterface.ORIENTATION_ROTATE_180:
							imageView.setRotation(180);
							break;
						case ExifInterface.ORIENTATION_ROTATE_270:
							imageView.setRotation(270);
							break;
						}
					}
				}
			}
		}
	}
}
