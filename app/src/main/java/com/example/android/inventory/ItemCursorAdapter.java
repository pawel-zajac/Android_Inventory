package com.example.android.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract.ItemEntry;
import com.example.android.inventory.utility.BitmapUtility;

/**
 * {@link ItemCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of item data as its data source. This adapter knows
 * how to create list items for each row of item data in the {@link Cursor}.
 */
public class ItemCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ItemCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ItemCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }


    /**
     * This method binds the item data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current item can be set on the item_name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        final long currentPosition = cursor.getPosition() + 1;
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.item_name);
        TextView priceTextView = (TextView) view.findViewById(R.id.item_price);
        TextView quantityTextView = (TextView) view.findViewById(R.id.item_quantity);
        Button saleButton = (Button) view.findViewById(R.id.sale_button);
        ImageView imageView = (ImageView) view.findViewById(R.id.item_image);

        // Find the columns of item attributes that we're interested in
        int idColumnIndex = cursor.getColumnIndex(ItemEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
        int imageColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE);

        //Get row id
        final int id = cursor.getInt(idColumnIndex);
        // Read the item attributes from the Cursor for the current item
        String itemName = cursor.getString(nameColumnIndex);
        int itemPrice = cursor.getInt(priceColumnIndex);
        final int itemQuantity = cursor.getInt(quantityColumnIndex);

        // Convert price in cents into x dollars, y cents format: x,y
        // if you use currency where larger unit does not consist of 100 smaller BE SURE TO MODIFY THIS
        final int centsInDolar = 100;
        int dollars = itemPrice / centsInDolar;
        int cents = itemPrice % centsInDolar;
        String centsPrefix = "";
        if (cents < 10) {
            centsPrefix = "0";
        }
        String convertedPrice = dollars + "," + centsPrefix + cents + context.getString(R.string.currency_symbol);

        //convert image stored as byte[] into bitmap
        Bitmap image = BitmapUtility.getImage(cursor.getBlob(imageColumnIndex));

        // Update the TextViews with the attributes for the current item
        nameTextView.setText(itemName);
        priceTextView.setText(convertedPrice);
        quantityTextView.setText(itemQuantity + "");
        if (image != null) {
            imageView.setImageBitmap(image);
        }

        // Get quantity of the current item and decrease it by 1 when user taps the button
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemQuantity > 0) {
                    Uri currentItem = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, id);
                    ContentValues values = new ContentValues();
                    values.put(ItemEntry.COLUMN_ITEM_QUANTITY, itemQuantity - 1);
                    int rowsAffected = context.getContentResolver().update(currentItem, values, null, null);

                    // Show a toast message depending on whether or not the update was successful.
                    if (rowsAffected != 1) {
                        // We wanted to update specific row, so if the number of affected rows is other than 1 then something went wrong
                        Toast.makeText(context, context.getString(R.string.update_error_toast) + rowsAffected, Toast.LENGTH_SHORT).show();
                    } else {
                        // Otherwise, the update was successful and we can display a toast.
                        Toast.makeText(context, context.getString(R.string.main_item_sold_toast), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
