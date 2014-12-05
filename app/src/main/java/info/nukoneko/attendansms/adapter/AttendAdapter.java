package info.nukoneko.attendansms.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import info.nukoneko.attendansms.R;
import info.nukoneko.attendansms.container.EntryObject;

import static info.nukoneko.attendansms.common.AttendanceUtil.unixTime2DateString;

/**
 * Created by Atsumi on 2014/12/03.
 */
public class AttendAdapter extends ArrayAdapter<EntryObject> {
    static class ViewHolder {
        TextView date;
        TextView studentID;
        TextView nameDetail;
        TextView name;
        TextView status;
    }
    private LayoutInflater mInflater;

    public AttendAdapter(Context context) {
        super(context, android.R.layout.activity_list_item);
        mInflater = (LayoutInflater) context.getSystemService((Activity.LAYOUT_INFLATER_SERVICE));
    }
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_attend_list, null);
            holder = new ViewHolder();
            holder.date = (TextView)convertView.findViewById(R.id.attend_day);
            holder.studentID = (TextView)convertView.findViewById(R.id.attend_id);
            holder.nameDetail = (TextView)convertView.findViewById(R.id.attend_name_detail);
            holder.name = (TextView)convertView.findViewById(R.id.attend_name);
            holder.status = (TextView)convertView.findViewById(R.id.attend_state);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        EntryObject item = getItem(position);

        holder.date.setText(unixTime2DateString(item.getTime()));
        holder.studentID.setText(item.getStudent().getUserID());
        holder.nameDetail.setText(item.getStudent().getFuriGana());
        holder.name.setText(item.getStudent().getFullName());
        holder.status.setText(item.getResult());

        if(!item.getWasAnim()) {
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.item_motion);
            convertView.startAnimation(anim);
            item.setWasAnim(true);
        }
        return convertView;
    }

    @Override
    public void add(EntryObject object){
        for(int i = 0; i < this.getCount(); i++){
            if(this.getItem(i).getStudent().getUserID().equals(object.getStudent().getUserID())) return;
        }
        super.add(object);
    }
}
