package nl.implode.weer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sander on 20-1-17.
 */
public class WeatherStationAutoCompleteAdapter extends BaseAdapter implements Filterable {

    private static final int MAX_RESULTS = 20;
    private Context mContext;
    private List<WeatherStation> resultList = new ArrayList<WeatherStation>();

    public WeatherStationAutoCompleteAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public WeatherStation getItem(int index) {
        return resultList.get(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //convertView = inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            convertView = inflater.inflate(R.layout.autocomplete_line, parent, false);
        }
        TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
        TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
        // Populate the data into the template view using the data object
        text1.setText(getItem(position).name + ", "+getItem(position).country);
        text2.setText(getItem(position).latitude + ", " + getItem(position).longitude);
        text2.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        final String lat = getItem(position).latitude;
        final String lon = getItem(position).longitude;
        text2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String url = "https://www.openstreetmap.org/#map=11/"+lat+"/"+lon;

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                mContext.startActivity(i);
            }
        });

        return convertView;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected Filter.FilterResults performFiltering(CharSequence constraint) {
                Filter.FilterResults filterResults = new Filter.FilterResults();
                if (constraint != null) {
                    List<WeatherStation> weatherStations = findWeatherStations(mContext, constraint.toString());

                    // Assign the data to the FilterResults
                    filterResults.values = weatherStations;
                    filterResults.count = weatherStations.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                if (results != null && results.count > 0) {
                    resultList = (List<WeatherStation>) results.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }};
        return filter;
    }

    /**
     * Returns a search result for the given a name.
     */
    private List<WeatherStation> findWeatherStations(Context context, String name) {
        WeatherStationsDatabase weatherStationsDatabase = new WeatherStationsDatabase(context);
        SQLiteDatabase db = weatherStationsDatabase.getReadableDatabase();
        String[] parts = name.split(",");
        String cityOrLat = parts[0];
        String countryOrLon = "";
        if (parts.length>1) {
            countryOrLon = parts[1].trim();
        }
        Cursor cursor = db.rawQuery("SELECT * FROM cities" +
                        " WHERE (name LIKE ? AND country LIKE ?)" +
                        " OR (lat LIKE ? AND lon LIKE ?) LIMIT 20;",
                new String[] {
                        "%"+cityOrLat+"%",
                        "%"+countryOrLon+"%",
                        cityOrLat+"%",
                        countryOrLon+"%"
                });
        List<WeatherStation> searchResults = new ArrayList<WeatherStation>();
        try {
            while (cursor.moveToNext()) {
                searchResults.add(new WeatherStation(
                        cursor.getInt(cursor.getColumnIndex("_id")),
                        cursor.getString(cursor.getColumnIndex("name")),
                        cursor.getString(cursor.getColumnIndex("country")),
                        cursor.getString(cursor.getColumnIndex("lat")),
                        cursor.getString(cursor.getColumnIndex("lon"))
                ));
            }
        } finally {
            cursor.close();
            db.close();
        }
        return searchResults;
    }
}

