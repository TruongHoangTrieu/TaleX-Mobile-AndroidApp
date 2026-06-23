package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.movies;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.movie.Actor;
import java.util.List;

public class ActorAdapter extends RecyclerView.Adapter<ActorAdapter.ActorViewHolder> {

    private final List<Actor> actorList;

    public ActorAdapter(List<Actor> actorList) {
        this.actorList = actorList;
    }

    @NonNull
    @Override
    public ActorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_actor, parent, false);
        return new ActorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActorViewHolder holder, int position) {
        Actor actor = actorList.get(position);
        holder.tvName.setText(actor.getName());
        holder.tvRole.setText(actor.getRole());
        holder.imgAvatar.setImageResource(actor.getAvatarResource());
    }

    @Override
    public int getItemCount() {
        return actorList != null ? actorList.size() : 0;
    }

    public static class ActorViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvRole;

        public ActorViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_actor_avatar);
            tvName = itemView.findViewById(R.id.tv_actor_name);
            tvRole = itemView.findViewById(R.id.tv_actor_role);
        }
    }
}
