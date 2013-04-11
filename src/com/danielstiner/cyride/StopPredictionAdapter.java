package com.danielstiner.cyride;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;
import com.danielstiner.cyride.util.TextFormat;

public class StopPredictionAdapter extends ArrayAdapter<StopPrediction> {

	public StopPredictionAdapter(Context context) {
		super(context, R.layout.stop_prediction_list_item, R.id.text_route);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);

		StopPrediction p = this.getItem(position);

		if (p != null) {
			TextView tr = (TextView) v.findViewById(R.id.text_route);
			TextView ts = (TextView) v.findViewById(R.id.text_stop);
			TextView tt = (TextView) v.findViewById(R.id.text_times);
			TextView td = (TextView) v.findViewById(R.id.text_distance);
			LinearLayout ll = (LinearLayout) v.findViewById(R.id.color);

			ll.setBackgroundColor(p.route.color);
			tr.setText(TextFormat.toString(p.route));
			ts.setText(TextFormat.toString(p.stop));
			tt.setText(TextFormat.toString(p.predictions));
			// TODO Fix
			td.setText("");
			//td.setText(TextFormat.distanceToString(p.stop.distance));
		}
		return v;
	}

}
