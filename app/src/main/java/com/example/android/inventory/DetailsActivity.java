package com.example.android.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract.ItemEntry;
import com.example.android.inventory.utility.BitmapUtility;

import java.io.IOException;

public class DetailsActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Variable keeping track on current activity state
     */
    private static int activityState;

    /**
     * Boolean flag that keeps track of whether the item has been edited (true) or not (false)
     */
    private boolean mItemHasChanged = false;

    /**
     * Value for 'new item' activity state
     */
    private static final int NEW_ITEM = 1000;

    /**
     * Value for 'item details' activity state
     */
    private static final int ITEM_DETAILS = 2000;

    /**
     * Value for 'edit item' activity state
     */
    private static final int EDIT_ITEM = 2001;

    /**
     * Identifier for the item data loader
     */
    private static final int EXISTING_ITEM_LOADER = 0;

    /**
     * Image capture request code
     */
    static final int REQUEST_IMAGE_CAPTURE = 1;

    /**
     * Image choice request code
     */
    static final int REQUEST_IMAGE_CHOICE = 2;

    /**
     * Content URI for the existing item (null if it's a new item)
     */
    private Uri mCurrentItemUri;

    /**
     * ImageView and background for current item image
     */
    private ImageView mItemImage;
    private ConstraintLayout mItemImageBackground;

    /**
     * textfield to enter the item's name
     */
    private EditText mNameEditText;
    private TextInputLayout mNameTextInputLayout;

    /**
     * textfield to enter the item's price
     */
    private EditText mPriceEditText;
    private TextInputLayout mPriceTextInputLayout;

    /**
     * textfield to enter the items's quantity
     */
    private EditText mQuantityEditText;
    private TextInputLayout mQuantityTextInputLayout;

    /**
     * textfield to enter the items's quantity
     */
    private EditText mContactEditText;
    private TextInputLayout mContactTextInputLayout;

    /**
     * String containing wholesaler's contact number
     */
    private String mContactNumber;

    /**
     * Buttons used to increment/decrement value in quantity textfield
     */
    private ImageButton mQuantityPlusButton;
    private ImageButton mQuantityMinusButton;

    /**
     * Temporaty photo tumbnail
     */
    private ImageView mItemNewImage;
    private LinearLayout mItemNewImageBackground;

    /**
     * Button used to open the camera app
     */
    private Button mCameraButton;

    /**
     * Button used to open the gallery app
     */
    private Button mGalleryButton;

    /**
     * Button used to update database with current item state
     */
    private Button mItemSaveButton;


    /**
     * Button used to dial number of the wholesaler
     */
    private Button mContactButton;

    /**
     * Floating action button
     */
    private FloatingActionButton mFab;

    /**
     * Chosen picture in Bitmap format
     */
    Bitmap mTakenPicture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Examine the intent that was used to launch this activity, set activity to corresponding state
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();
        if (mCurrentItemUri == null) {
            activityState = NEW_ITEM;
        } else {
            activityState = ITEM_DETAILS;
        }

        // Find views relevant for current activity state
        findViews();
        // Set views states etc according to the current activity state
        setViewsStates();
        // Setup OnTouchListeners on input fields to track user input
        setOnTouchListeners();
        // Set Listeners to unlock features according to the current activity state
        setFeatures();

        // Initialize a loader to read the item data from the database and display the current values in the views
        if (activityState == ITEM_DETAILS) {
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            mTakenPicture = (Bitmap) extras.get("data");
            mItemNewImage.setImageBitmap(mTakenPicture);
        }
        if (requestCode == REQUEST_IMAGE_CHOICE && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = ThumbnailUtils.extractThumbnail(MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri), 300, 300);
                mTakenPicture = bitmap;
                mItemNewImage.setImageBitmap(mTakenPicture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu layout to adds items to the app bar
        getMenuInflater().inflate(R.menu.details_menu, menu);
        // Show delete menu item if existing item is viewed
        MenuItem deleteItem = menu.findItem(R.id.action_delete);
        if (activityState == NEW_ITEM) {
            deleteItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item hasn't changed, continue with navigating up to parent activity
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailsActivity.this);
                    return true;
                }
                // If there are unsaved changes, setup a dialog to warn the user.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(DetailsActivity.this);
                            }
                        };
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder, set the message, and click listeners for the positive and negative buttons
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.details_unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.details_unsaved_changes_discard_option, discardButtonClickListener);
        builder.setNegativeButton(R.string.details_unsaved_changes_stay_option, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Stay" button, dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this item
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder, set the message, and click listeners  for the positive and negative buttons
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.details_delete_dialog_msg);
        builder.setPositiveButton(R.string.details_delete_dialog_delete_option, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, delete the item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.details_delete_dialog_cancel_option, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the item in the database
     */
    private void deleteItem() {
        // Only perform the delete if this is an existing item.
        if (activityState == ITEM_DETAILS || activityState == EDIT_ITEM) {
            // Call the ContentResolver to delete the item at the given content URI
            // Pass in null for the selection and selection args - mCurrentItemUri identifies the item
            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);
            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.main_delete_failure_toast), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.main_delete_success_toast), Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define a projection that specifies the columns from the table are needed
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_PRICE,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_CONTACT,
                ItemEntry.COLUMN_ITEM_IMAGE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Finish early if the cursor is null or it has number of rows different from 1
        if (data == null || data.getCount() != 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        if (data.moveToFirst()) {
            // Find the columns of item attributes that we're interested in
            int nameColumnIndex = data.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int priceColumnIndex = data.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
            int quantityColumnIndex = data.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
            int contactColumnIndex = data.getColumnIndex(ItemEntry.COLUMN_ITEM_CONTACT);
            int imageColumnIndex = data.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE);

            // Extract out the value from the Cursor for the given column index
            String name = data.getString(nameColumnIndex);
            int price = data.getInt(priceColumnIndex);
            int quantity = data.getInt(quantityColumnIndex);
            long contact = data.getLong(contactColumnIndex);
            Bitmap image = BitmapUtility.getImage(data.getBlob(imageColumnIndex));

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mPriceEditText.setText(Integer.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));
            mContactEditText.setText(Long.toString(contact));
            mContactNumber = Long.toString(contact);
            if (image != null) {
                mItemImage.setImageBitmap(image);
                mItemNewImage.setImageBitmap(image);
            } else {
                mItemImage.setVisibility(View.GONE);
                mItemImageBackground.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mContactEditText.setText("");
        mItemImage.setImageBitmap(null);
        mItemNewImage.setImageBitmap(null);
    }

    /**
     * Find and assign views relevant to the current activity state
     */
    private void findViews() {
        mItemImage = (ImageView) findViewById(R.id.details_item_image);
        mItemImageBackground = (ConstraintLayout) findViewById(R.id.details_item_image_background);
        mNameTextInputLayout = (TextInputLayout) findViewById(R.id.details_name_input_layout);
        mNameEditText = (EditText) findViewById(R.id.details_name_window);
        mPriceTextInputLayout = (TextInputLayout) findViewById(R.id.details_price_input_layout);
        mPriceEditText = (EditText) findViewById(R.id.details_price_window);
        mQuantityTextInputLayout = (TextInputLayout) findViewById(R.id.details_quantity_input_layout);
        mQuantityEditText = (EditText) findViewById(R.id.details_quantity_window);
        mContactEditText = (EditText) findViewById(R.id.details_contact_window);
        mItemNewImage = (ImageView) findViewById(R.id.details_item_new_image);

        switch (activityState) {
            case NEW_ITEM:
                mContactTextInputLayout = (TextInputLayout) findViewById(R.id.details_contact_input_layout);
                mQuantityPlusButton = (ImageButton) findViewById(R.id.details_quantity_plus_button);
                mQuantityMinusButton = (ImageButton) findViewById(R.id.details_quantity_minus_button);
                mItemNewImageBackground = (LinearLayout) findViewById(R.id.details_item_new_image_background);
                mCameraButton = (Button) findViewById(R.id.details_camera_button);
                mGalleryButton = (Button) findViewById(R.id.details_gallery_button);
                mItemSaveButton = (Button) findViewById(R.id.details_item_save_button);

                break;
            case ITEM_DETAILS:
                mContactButton = (Button) findViewById(R.id.details_contact_button);
                mFab = (FloatingActionButton) findViewById(R.id.details_item_edit_fab);
                break;
            case EDIT_ITEM:
                mContactTextInputLayout = (TextInputLayout) findViewById(R.id.details_contact_input_layout);
                mQuantityPlusButton = (ImageButton) findViewById(R.id.details_quantity_plus_button);
                mQuantityMinusButton = (ImageButton) findViewById(R.id.details_quantity_minus_button);
                mItemNewImageBackground = (LinearLayout) findViewById(R.id.details_item_new_image_background);
                mCameraButton = (Button) findViewById(R.id.details_camera_button);
                mGalleryButton = (Button) findViewById(R.id.details_gallery_button);
                mItemSaveButton = (Button) findViewById(R.id.details_item_save_button);
                break;
            default:
                throw new IllegalStateException("No valid activity state recognized");
        }
    }

    /**
     * Set onTouchListener on views relevant for current activity state
     */
    private void setOnTouchListeners() {
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mItemHasChanged = true; //indicate user-made changes
                return false;
            }
        };
        switch (activityState) {
            case NEW_ITEM:  //fallthrough
            case EDIT_ITEM:
                mNameEditText.setOnTouchListener(touchListener);
                mPriceEditText.setOnTouchListener(touchListener);
                mQuantityEditText.setOnTouchListener(touchListener);
                mQuantityPlusButton.setOnTouchListener(touchListener);
                mQuantityMinusButton.setOnTouchListener(touchListener);
                mContactEditText.setOnTouchListener(touchListener);
                mCameraButton.setOnTouchListener(touchListener);
                mGalleryButton.setOnTouchListener(touchListener);
                break;
            case ITEM_DETAILS:
                //No listening needed until user enables editing
                break;
            default:
                throw new IllegalStateException("No valid activity state recognized");
        }
    }

    /**
     * Set views states according to the current activity state
     */
    private void setViewsStates() {
        switch (activityState) {
            case NEW_ITEM:
                // Set activity title according to the current activity state
                setTitle(getString(R.string.details_add_item_title));

                // Set views state according to the current activity state
                mNameEditText.setFocusableInTouchMode(true);
                mPriceEditText.setFocusableInTouchMode(true);
                mQuantityEditText.setFocusableInTouchMode(true);
                mContactTextInputLayout.setVisibility(View.VISIBLE);
                mContactEditText.setFocusableInTouchMode(true);
                mQuantityPlusButton.setVisibility(View.VISIBLE);
                mQuantityMinusButton.setVisibility(View.VISIBLE);
                mItemNewImage.setVisibility(View.VISIBLE);
                mItemNewImageBackground.setVisibility(View.VISIBLE);
                mCameraButton.setVisibility(View.VISIBLE);
                mGalleryButton.setVisibility(View.VISIBLE);
                mItemSaveButton.setVisibility(View.VISIBLE);

                // Set button text according to the current activity state
                mItemSaveButton.setText(R.string.details_add_item_save_button);
                break;
            case ITEM_DETAILS:
                // Set activity title according to the current activity state
                setTitle(getString(R.string.details_existing_item_title));

                // Set views state according to the current activity state
                mItemImage.setVisibility(View.VISIBLE);
                mContactButton.setVisibility(View.VISIBLE);
                mFab.setVisibility(View.VISIBLE);
                mItemImageBackground.setVisibility(View.VISIBLE);
                break;
            case EDIT_ITEM:
                // Set activity title according to the current activity state
                setTitle(getString(R.string.details_edit_item_title));

                // Set views state according to the current activity state
                mItemImage.setVisibility(View.GONE);
                mContactButton.setVisibility(View.GONE);
                mFab.setVisibility(View.GONE);
                mItemImageBackground.setVisibility(View.GONE);
                mNameEditText.setFocusableInTouchMode(true);
                mPriceEditText.setFocusableInTouchMode(true);
                mQuantityEditText.setFocusableInTouchMode(true);
                mContactTextInputLayout.setVisibility(View.VISIBLE);
                mContactEditText.setFocusableInTouchMode(true);
                mQuantityPlusButton.setVisibility(View.VISIBLE);
                mQuantityMinusButton.setVisibility(View.VISIBLE);
                mItemNewImage.setVisibility(View.VISIBLE);
                mItemNewImageBackground.setVisibility(View.VISIBLE);
                mCameraButton.setVisibility(View.VISIBLE);
                mGalleryButton.setVisibility(View.VISIBLE);
                mItemSaveButton.setVisibility(View.VISIBLE);

                // Set button text according to the current activity state
                mItemSaveButton.setText(R.string.details_existing_item_update_button);
                break;
            default:
                throw new IllegalStateException("No valid activity state recognized");
        }
    }

    /**
     * Set onClickListeners on views according to the current activity state
     */
    private void setFeatures() {
        switch (activityState) {
            case NEW_ITEM:
                setQuantityControls();
                setImageChooser();
                setSaveFeature();
                break;
            case ITEM_DETAILS:
                setContactFeature();
                setEditFeature();
                break;
            case EDIT_ITEM:
                setQuantityControls();
                setImageChooser();
                setSaveFeature();
                break;
            default:
                throw new IllegalStateException("No valid activity state recognized");
        }
    }

    /**
     * Validate user input and set errors accordingly
     */
    private boolean isUserInputValid() {
        boolean allFieldsValid = true;

        if (TextUtils.isEmpty(mNameEditText.getText().toString().trim())) {
            allFieldsValid = false;
            mNameTextInputLayout.setError(getString(R.string.details_name_input_error));
        } else {
            mNameTextInputLayout.setError(null);
        }
        if (TextUtils.isEmpty(mPriceEditText.getText().toString()) || Integer.parseInt(mPriceEditText.getText().toString()) <= 0) {
            allFieldsValid = false;
            mPriceTextInputLayout.setError(getString(R.string.details_price_input_error));
        } else {
            mPriceTextInputLayout.setError(null);
        }
        if (TextUtils.isEmpty(mQuantityEditText.getText().toString()) || Integer.parseInt(mQuantityEditText.getText().toString()) < 0) {
            allFieldsValid = false;
            mQuantityTextInputLayout.setError(getString(R.string.details_quantity_input_error));
        } else {
            mQuantityTextInputLayout.setError(null);
        }
        if (TextUtils.isEmpty(mContactEditText.getText().toString())) {
            allFieldsValid = false;
            mContactTextInputLayout.setError(getString(R.string.details_contact_input_error));
        } else {
            mContactTextInputLayout.setError(null);
        }
        return allFieldsValid;
    }

    /**
     * Get user input so it can be used in database operation
     */
    private ContentValues getContentValues() {
        //If there is a picutre to be added to db convert it into byteArray
        byte[] image = BitmapUtility.getBytes(mTakenPicture);

        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_NAME, mNameEditText.getText().toString().trim());
        values.put(ItemEntry.COLUMN_ITEM_PRICE, Integer.parseInt(mPriceEditText.getText().toString()));
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, Integer.parseInt(mQuantityEditText.getText().toString()));
        values.put(ItemEntry.COLUMN_ITEM_CONTACT, Long.parseLong(mContactEditText.getText().toString()));
        values.put(ItemEntry.COLUMN_ITEM_IMAGE, image);
        return values;
    }

    /**
     * Set onClickListeners on quantity +/- buttons
     */
    private void setQuantityControls() {
        mQuantityPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mQuantityEditText.getText().toString())) {
                    mQuantityEditText.setText("1");
                } else if (Integer.parseInt(mQuantityEditText.getText().toString()) < 999999999){
                    mQuantityEditText.setText(Integer.toString(Integer.parseInt(mQuantityEditText.getText().toString()) + 1));
                }
            }
        });
        mQuantityMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mQuantityEditText.getText().toString())) {
                    mQuantityEditText.setText("0");
                } else if (Integer.parseInt(mQuantityEditText.getText().toString()) > 0) {
                    mQuantityEditText.setText(Integer.toString(Integer.parseInt(mQuantityEditText.getText().toString()) - 1));
                }
            }
        });
    }

    /**
     * Set onClickListeners on photo button to get a picture
     */
    private void setImageChooser() {
        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        mGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, REQUEST_IMAGE_CHOICE);
                }
            }
        });

    }

    /**
     * Set onClickListener on save button to validate user input and insert/update the database
     */
    private void setSaveFeature() {
        mItemSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Disable save button so user doesnt trigger multiple actions
                mItemSaveButton.setEnabled(false);

                //Bail early if one of the fields has invalid input or user did not enter any data
                if (!isUserInputValid()) {
                    Toast.makeText(DetailsActivity.this, getString(R.string.details_invalid_fields_toast), Toast.LENGTH_SHORT).show();
                    mItemSaveButton.setEnabled(true);
                    return;
                } else if (!mItemHasChanged) {
                    Toast.makeText(DetailsActivity.this, getString(R.string.details_item_no_changes), Toast.LENGTH_SHORT).show();
                    mItemSaveButton.setEnabled(true);
                    return;
                }

                //Insert or update depending on activity state
                switch (activityState) {
                    case NEW_ITEM:
                        Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, getContentValues());
                        if (newUri != null) {
                            Toast.makeText(DetailsActivity.this, getString(R.string.main_save_item_success_toast), Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        break;
                    case EDIT_ITEM:
                        int rowsModified = getContentResolver().update(mCurrentItemUri, getContentValues(), null, null);
                        if (rowsModified == 1) {
                            Toast.makeText(DetailsActivity.this, getString(R.string.main_update_item_success_toast), Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        break;
                    default:
                        throw new IllegalStateException("No valid activity state recognized");
                }
                Toast.makeText(DetailsActivity.this, getString(R.string.main_something_wrong_toast), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Set onClickListener on contact button to dial the number
     */
    private void setContactFeature() {
        mContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mContactNumber));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * Set onClickListener on FAB to enter edit mode
     */
    private void setEditFeature() {
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityState = EDIT_ITEM;
                findViews();
                setViewsStates();
                setOnTouchListeners();
                setFeatures();
            }
        });
    }

}
