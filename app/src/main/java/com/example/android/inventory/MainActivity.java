package com.example.android.inventory;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.inventory.data.InventoryContract.ItemEntry;

/**
 * Displays list of items that were entered and stored in the app.
 */
public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /** Identifier for the item data loader */
    private static final int ITEM_LOADER = 0;

    /** ListView */
    private ListView mItemListView;

    /** Adapter for the ListView */
    private ItemCursorAdapter mCursorAdapter;

    /** Floating aciton button */
    private FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the ListView which will be populated with the item data
        mItemListView = (ListView) findViewById(R.id.main_item_list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.main_empty_view);
        mItemListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of item data in the Cursor.
        // There is no item data yet (until the loader finishes) so pass in null for the Cursor.
        mCursorAdapter = new ItemCursorAdapter(this, null);
        mItemListView.setAdapter(mCursorAdapter);

        // Set Listeners to unlock features
        setAddFeature();
        setDetailsFeature();

        // Initiate the loader
        getLoaderManager().initLoader(ITEM_LOADER, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-enable buttons
        mItemListView.setEnabled(true);
        mFab.setEnabled(true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table that are needed.
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_PRICE,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_IMAGE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                ItemEntry.CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link ItemCursorAdapter} with this new cursor containing updated item data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }

    /** Set onClickListener on FAB to go to item adding */
    private void setAddFeature() {
        mFab = (FloatingActionButton) findViewById(R.id.main_item_add_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Disable FAB taps so the user wont open one than more details screen
                mFab.setEnabled(false);
                // Create new intent to go to {@link DetailsActivity}
                Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                startActivity(intent);
            }
        });
    }

    /** Set onItemClickListener on list items to go to item details */
    private void setDetailsFeature() {
        mItemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Disable itemList taps so the user wont open one than more details screen
                mItemListView.setEnabled(false);
                // Create new intent to go to {@link DetailsActivity}
                Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                // Form the content URI that represents the specific item that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link ItemEntry#CONTENT_URI}.
                // For example, the URI would be "content://com.example.android.inventory/items/2"
                // if the item with ID 2 was clicked on.
                Uri currentUri = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, id);
                // Set the URI on the data field of the intent
                intent.setData(currentUri);
                startActivity(intent);
            }
        });
    }


}
