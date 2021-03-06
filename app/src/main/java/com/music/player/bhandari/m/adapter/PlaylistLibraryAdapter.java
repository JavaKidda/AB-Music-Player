package com.music.player.bhandari.m.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElementHelper.ColorHelper;
import com.music.player.bhandari.m.activity.ActivityMain;
import com.music.player.bhandari.m.activity.ActivitySecondaryLibrary;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.dataItem;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.model.MusicLibrary;
import com.music.player.bhandari.m.MyApp;
import com.music.player.bhandari.m.model.PlaylistManager;

import java.io.File;
import java.util.ArrayList;

/**
 Copyright 2017 Amit Bhandari AB

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

public class PlaylistLibraryAdapter extends RecyclerView.Adapter<PlaylistLibraryAdapter.MyViewHolder>
        implements PopupMenu.OnMenuItemClickListener{

    private ArrayList<String> headers=new ArrayList<>();
    private Context context;
    private LayoutInflater inflater;
    private int position=0;
    private PlayerService playerService;
    private View viewParent;


    public PlaylistLibraryAdapter(Context context){
        //create first page for folder fragment
        this.context=context;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        inflater=LayoutInflater.from(context);
        headers = PlaylistManager.getInstance(context).GetPlaylistList(false);
        playerService = MyApp.getService();
        setHasStableIds(true);
    }

    public void clear(){
    }

    public void refreshPlaylistList(){
        headers = PlaylistManager.getInstance(context).GetPlaylistList(false);
        notifyDataSetChanged();
    }
    @Override
    public PlaylistLibraryAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.fragment_library_item, parent, false);
        viewParent = parent;
        final PlaylistLibraryAdapter.MyViewHolder holder=new PlaylistLibraryAdapter.MyViewHolder (view);
        int color = ColorHelper.getBaseThemeTextColor() ;
        ((TextView)(view.findViewById(R.id.header))).setTextColor(color);
        ((TextView)(view.findViewById(R.id.secondaryHeader))).setTextColor(color);
        ((TextView)(view.findViewById(R.id.count))).setTextColor(color);
        ((ImageView)(view.findViewById(R.id.menuPopup))).setColorFilter(color);


        return holder;
    }

    @Override
    public void onBindViewHolder(PlaylistLibraryAdapter.MyViewHolder holder, int position) {
        holder.title.setText(headers.get(position));
        holder.title.setPadding(20,0,0,0);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_play:
                if(MyApp.isLocked()){
                    //Toast.makeText(context,"Music is Locked!",Toast.LENGTH_SHORT).show();
                    Snackbar.make(viewParent, context.getString(R.string.music_is_locked), Snackbar.LENGTH_LONG).show();
                    return true;
                }
                Play();
                break;

            case R.id.action_share:
                Share();
                break;

            case R.id.action_delete:
                Delete();
                break;

            case R.id.action_play_next:
                AddToQ(Constants.ADD_TO_Q.IMMEDIATE_NEXT);
                break;

            case R.id.action_add_to_q:
                AddToQ(Constants.ADD_TO_Q.AT_LAST);
                break;

            case R.id.action_clear_playlist:
                if(PlaylistManager.getInstance(context).ClearPlaylist(headers.get(position))){
                    Snackbar.make(viewParent, context.getString(R.string.snack_cleared) + " " + headers.get(position), Snackbar.LENGTH_LONG).show();
                }else {
                    Snackbar.make(viewParent, context.getString(R.string.snack_unable_to_Clear) + " " + headers.get(position), Snackbar.LENGTH_LONG).show();
                }
                break;
        }
        return true;
    }

    private void Play(){
        ArrayList<dataItem> temp = PlaylistManager.getInstance(context).GetPlaylist(headers.get(position));
        ArrayList<Integer> trackList = new ArrayList<>();
        for(dataItem d:temp){
            trackList.add(d.id);
        }

        if(!trackList.isEmpty()) {
            playerService.setTrackList(trackList);
            playerService.playAtPosition(0);
            /*
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                    .setAction(Constants.ACTION.PLAY_AT_POSITION)
                    .putExtra("position",0));*/
        }else {
            //Toast.makeText(context,"empty playlist",Toast.LENGTH_SHORT).show();
            Snackbar.make(viewParent, context.getString(R.string.empty_play_list), Snackbar.LENGTH_LONG).show();
        }
    }

    private void Share(){
        ArrayList<Uri> files = new ArrayList<>();  //for sending multiple files
        ArrayList<dataItem> temp = PlaylistManager.getInstance(context).GetPlaylist(headers.get(position));
        ArrayList<Integer> trackList = new ArrayList<>();
        for(dataItem d:temp){
            trackList.add(d.id);
        }
        for( int id : trackList){
            try {
                File file = new File(MusicLibrary.getInstance().getTrackItemFromId(id).getFilePath());
                Uri fileUri =
                        FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + "com.bhandari.music.provider", file);
                files.add(fileUri);
            }
            catch (Exception e ){
                //Toast.makeText(context,"Something wrong!",Toast.LENGTH_LONG).show();
                Snackbar.make(viewParent, context.getString(R.string.error_something_wrong), Snackbar.LENGTH_LONG).show();
                return;
            }
        }
        if(!files.isEmpty()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("*/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
            context.startActivity(Intent.createChooser(intent, "multiple audio files"));
        }else {
            //Toast.makeText(context,"empty playlist",Toast.LENGTH_SHORT).show();
            Snackbar.make(viewParent, context.getString(R.string.empty_play_list), Snackbar.LENGTH_LONG).show();
        }
    }

    private void AddToQ(int positionToAdd){
        //we are using same function for adding to q and playing next
        // toastString is to identify which string to disokay as toast
        String toastString=(positionToAdd==Constants.ADD_TO_Q.AT_LAST ? context.getString(R.string.added_to_q)
                : context.getString(R.string.playing_next)) ;
        //when adding to playing next, order of songs should be desc
        //and asc for adding at last
        //this is how the function in player service is writte, deal with it
        int sortOrder=(positionToAdd==Constants.ADD_TO_Q.AT_LAST ? Constants.SORT_ORDER.ASC : Constants.SORT_ORDER.DESC);

        ArrayList<dataItem> temp = PlaylistManager.getInstance(context).GetPlaylist(headers.get(position));
        ArrayList<Integer> trackList = new ArrayList<>();
        for(dataItem d:temp){
            trackList.add(d.id);
        }
        if(!trackList.isEmpty()) {
            for (int id : trackList) {
                playerService.addToQ(id, positionToAdd);
            }
            //to update the to be next field in notification
            MyApp.getService().PostNotification();

            /*Toast.makeText(context
                    , toastString + headers.get(position)
                    , Toast.LENGTH_SHORT).show();*/
            Snackbar.make(viewParent, toastString + headers.get(position), Snackbar.LENGTH_LONG).show();
        }else {
            //Toast.makeText(context,"empty playlist",Toast.LENGTH_SHORT).show();
            Snackbar.make(viewParent, context.getString(R.string.empty_play_list), Snackbar.LENGTH_LONG).show();
        }
    }

    private void Delete(){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //if tried deleting system playlist, give error
                        if(headers.get(position).equals(Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED)
                                || headers.get(position).equals(Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED)
                                || headers.get(position).equals(Constants.SYSTEM_PLAYLISTS.MOST_PLAYED)
                                || headers.get(position).equals(Constants.SYSTEM_PLAYLISTS.MY_FAV))
                        {
                            //Toast.makeText(context,"Cannot delete "+headers.get(position),Toast.LENGTH_SHORT).show();
                            Snackbar.make(viewParent, context.getString(R.string.cannot_del)+headers.get(position), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        if(PlaylistManager.getInstance(context).DeletePlaylist(headers.get(position))){
                            //Toast.makeText(context,"Deleted "+headers.get(position),Toast.LENGTH_SHORT).show();
                            Snackbar.make(viewParent, context.getString(R.string.deleted)+headers.get(position), Snackbar.LENGTH_LONG).show();
                            headers.remove(headers.get(position));
                            notifyDataSetChanged();
                        }else {
                            //Toast.makeText(context,"Cannot delete "+headers.get(position),Toast.LENGTH_SHORT).show();
                            Snackbar.make(viewParent, context.getString(R.string.cannot_del)+headers.get(position), Snackbar.LENGTH_LONG).show();
                        }
                        //Yes button clicked
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getString(R.string.are_u_sure))
                .setPositiveButton(context.getString(R.string.yes), dialogClickListener)
                .setNegativeButton(context.getString(R.string.no), dialogClickListener).show();
    }
    @Override
    public int getItemCount() {
        return headers.size();
    }

    public void onClick(View view, int position) {
        this.position=position;
        switch (view.getId()){
            case R.id.trackItem:
                Intent intent = new Intent(context,ActivitySecondaryLibrary.class);
                intent.putExtra("status",Constants.FRAGMENT_STATUS.PLAYLIST_FRAGMENT);
                intent.putExtra("title",headers.get(position).trim());
                context.startActivity(intent);
                ((ActivityMain)context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;

            case R.id.menuPopup:
                PopupMenu popup = new PopupMenu(context, view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.playlist_menu, popup.getMenu());
                popup.show();
                popup.setOnMenuItemClickListener(this);
                break;
        }
    }


    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title;

        public MyViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.header);
            itemView.findViewById(R.id.album_art_wrapper).setVisibility(View.GONE);
            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.menuPopup).setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            PlaylistLibraryAdapter.this.onClick(view,getLayoutPosition());
        }
    }
}
