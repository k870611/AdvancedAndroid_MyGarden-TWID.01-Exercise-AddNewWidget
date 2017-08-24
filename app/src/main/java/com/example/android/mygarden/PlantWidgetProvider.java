package com.example.android.mygarden;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.ui.MainActivity;
import com.example.android.mygarden.ui.PlantDetailActivity;

/**
 * Implementation of App Widget functionality.
 */
public class PlantWidgetProvider extends AppWidgetProvider {

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int imgRes, long plantId, boolean showWater, int appWidgetId) {
        // TODO (18): separate the updateAppWidget logic into getGardenGridRemoteView and getSinglePlantRemoteView
        // Get current width to decide on single plant vs garden grid view
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        RemoteViews rv;
        if (width < 300) {
            rv = getSinglePlantRemoteView(context, imgRes, plantId, showWater);
        } else {
            rv = getGardenGridRemoteView(context);
        }
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    private static RemoteViews getSinglePlantRemoteView(Context context, int imgRes, long plantId, boolean showWater) {
        // TODO (16): Set the click handler to open the DetailActivity for plant ID,
        // or the MainActivity if plant ID is invalid
        // Create an Intent to launch MainActivity when clicked
        Intent intent;
        if (plantId == PlantContract.INVALID_PLANT_ID) {
            intent = new Intent(context, MainActivity.class);
        } else { // Set on click to open the corresponding detail activity
            Log.d(PlantWidgetProvider.class.getSimpleName(), "plantId=" + plantId);
            intent = new Intent(context, PlantDetailActivity.class);
            intent.putExtra(PlantDetailActivity.EXTRA_PLANT_ID, plantId);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.plant_widget);

        /*TODO (3): Create a PlantWidgetProvider class that extends AppWidgetProvider and (Replace by 16)
                    set updateAppWidget to handle clicks and launch MainActivity*/

        //Create an Intent to launch MainActivity when clicked (Replace by 16)
        //Intent intent = new Intent(context, MainActivity.class);
        //PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);

        // TODO (12): Modify updateAppWidget method to take an image recourse and call
        // setImageViewResource to update the widget’s image

        // Update image
        views.setImageViewResource(R.id.widget_plant_image, imgRes);

        // Update plant ID text
        views.setTextViewText(R.id.widget_plant_name, String.valueOf(plantId));
        // Show/Hide the water drop button
        if (showWater) views.setViewVisibility(R.id.widget_water_button, View.VISIBLE);
        else views.setViewVisibility(R.id.widget_water_button, View.INVISIBLE);
        // Widgets allow click handlers to only launch pending intents

        // Widgets allow click handles to only launch pending intents
        views.setOnClickPendingIntent(R.id.widget_plant_image, pendingIntent);

        // TODO (9): Create a PendingIntent for the PlantWateringService and setOnClickPendingIntent for widget_water_button
        // Add the wateringservice click handler
        Intent wateringIntent = new Intent(context, PlantWateringService.class);
        wateringIntent.setAction(PlantWateringService.ACTION_WATER_PLANT);
        wateringIntent.putExtra(PlantWateringService.EXTRA_PLANT_ID, plantId);
        PendingIntent wateringPendingIntent = PendingIntent.getService(
                context,
                0,
                wateringIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.widget_water_button, wateringPendingIntent);
        return views;
    }

    // TODO (23): Set the PendingIntent template in getGardenGridRemoteView to launch PlantDetailActivity
    private static RemoteViews getGardenGridRemoteView(Context context) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_grid_view);
        // Set the GridWidgetService intent to act as the adapter for the GridView
        Intent intent = new Intent(context, GridWidgetService.class);
        views.setRemoteAdapter(R.id.widget_grid_view, intent);

        // Set the PlantDetailActivity intent to launch when clicked
        Intent appIntent = new Intent(context, PlantDetailActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widget_grid_view, appPendingIntent);

        // Handle empty gardens
        views.setEmptyView(R.id.widget_grid_view, R.id.empty_view);
        return views;
    }

    // There may be multiple widgets active, so update all of them
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // TODO (13): Call startActionUpdatePlantWidgets in onUpdate as well as in AddPlantActivity and PlantDetailActivity (add and delete plants)
        //Start the intent service update widget action, the service takes care of updating the widgets UI
        PlantWateringService.startActionUpdatePlantWidgets(context);
    }

    // TODO (11): Move the updateAppWidget loop to a new method called updatePlantWidgets and pass through the image recourse
    public static void updatePlantWidgets(Context context, AppWidgetManager appWidgetManager,
                                          int imgRes, long plantId, boolean showWater, int[] appWidgetIds) {
        // TODO (15): Modify updatePlantWidgets and updateAppWidget to pass the plant ID as well as a boolean
        // to show/hide the water button

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, imgRes, plantId, showWater, appWidgetId);
        }
    }

    // TODO (19): Use getAppWidgetOptions to get widget width and use the appropriate RemoteView method
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        PlantWateringService.startActionUpdatePlantWidgets(context);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

