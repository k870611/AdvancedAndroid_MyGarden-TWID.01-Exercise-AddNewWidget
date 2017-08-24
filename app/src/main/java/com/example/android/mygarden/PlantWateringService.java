package com.example.android.mygarden;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.utils.PlantUtils;

import static com.example.android.mygarden.provider.PlantContract.BASE_CONTENT_URI;
import static com.example.android.mygarden.provider.PlantContract.INVALID_PLANT_ID;
import static com.example.android.mygarden.provider.PlantContract.PATH_PLANTS;

/**
 * Created by Huang on 2017/8/24.
 */

public class PlantWateringService extends IntentService {
     /*TODO (7): Create a plant watering service that extends IntentService and supports the
            action ACTION_WATER_PLANTS which updates last_watered timestamp for all plants still alive */

    public PlantWateringService() {
        super("PlantWateringService");
    }

    // TODO (14): Change ACTION_WATER_PLANTS to ACTION_WATER_PLANT and
    // use EXTRA_PLANT_ID to pass the plant ID to the service and update the query to use SINGLE_PLANT_URI
    public static final String ACTION_WATER_PLANT = "com.example.android.mygarden.action.water_plants";
    public static final String EXTRA_PLANT_ID = "com.example.android.mygarden.extra.PLANT_ID";;

    public static void startActionWaterPlans(Context context, long plantId){
        Intent intent = new Intent(context,PlantWateringService.class);
        intent.setAction(ACTION_WATER_PLANT);
        intent.putExtra(EXTRA_PLANT_ID, plantId);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent !=null){
            final String action = intent.getAction();
            if (ACTION_WATER_PLANT.equals(action)) {
                final long plantId = intent.getLongExtra(EXTRA_PLANT_ID,
                        PlantContract.INVALID_PLANT_ID);
                handleActionWaterPlant(plantId);
            } else if (ACTION_UPDATE_PLANT_WIDGETS.equals(action)) {
                handleActionUpdatePlantWidgets();
            }
        }
    }

    private void handleActionWaterPlant(long plantId) {
        Uri SINGLE_PLANT_URI = ContentUris.withAppendedId(
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build(), plantId);
        ContentValues contentValues = new ContentValues();
        long timeNow = System.currentTimeMillis();
        contentValues.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);
        // Update only if that plant is still alive
        getContentResolver().update(
                SINGLE_PLANT_URI,
                contentValues,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME + ">?",
                new String[]{String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)});
        // Always update widgets after watering plants
        startActionUpdatePlantWidgets(this);
    }

    private void handleActionWaterPlants() {
        Uri PLANTS_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();
        ContentValues contentValues = new ContentValues();
        long timeNow = System.currentTimeMillis();
        contentValues.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME,timeNow);

        //Update only plants that are still alive
        getContentResolver().update(
                PLANTS_URI,
                contentValues,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME+">?",
                new String[]{String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)}
        );
    }

    // TODO (10): Create a new action ACTION_UPDATE_PLANT_WIDGETS to handle updating widget UI and
    public static final String ACTION_UPDATE_PLANT_WIDGETS = "com.example.android.mygarden.action.update_plant_widgets";

    public static void startActionUpdatePlantWidgets(Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_UPDATE_PLANT_WIDGETS);
        context.startService(intent);
    }

    private void handleActionUpdatePlantWidgets() {
        //Query to get the plant that's most in need for water (last watered)
        Uri PLANT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();
        Cursor cursor = getContentResolver().query(
                PLANT_URI,
                null,
                null,
                null,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME
        );
        // Extract the plant details
        int imgRes = R.drawable.grass; // Default image in case our garden is empty

        boolean canWater = false; // Default to hide the water drop button
        long plantId = INVALID_PLANT_ID;

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int createTimeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_CREATION_TIME);
            int waterTimeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
            int plantTypeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_PLANT_TYPE);

            long timeNow = System.currentTimeMillis();
            long wateredAt = cursor.getLong(waterTimeIndex);
            long createdAt = cursor.getLong(createTimeIndex);
            int plantType = cursor.getInt(plantTypeIndex);

            int idIndex = cursor.getColumnIndex(PlantContract.PlantEntry._ID);
            plantId = cursor.getLong(idIndex);

            cursor.close();

            canWater = (timeNow - wateredAt) > PlantUtils.MIN_AGE_BETWEEN_WATER &&
                    (timeNow - wateredAt) < PlantUtils.MAX_AGE_WITHOUT_WATER;

            imgRes = PlantUtils.getPlantImageRes(this, timeNow - createdAt, timeNow - wateredAt, plantType);
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, PlantWidgetProvider.class));

        // TODO (22):Trigger data update to handle the Gridview widget and force a data refresh
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_grid_view);


        //Now update all widgets
        PlantWidgetProvider.updatePlantWidgets(this, appWidgetManager, imgRes,plantId ,canWater,appWidgetIds);
    }
}
