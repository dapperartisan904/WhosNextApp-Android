package com.app.whosnextapp.drawer.homepage;

import android.content.Context;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.whosnextapp.R;
import com.app.whosnextapp.drawer.ViewCommentAdapter;
import com.app.whosnextapp.model.GetAllDashboardData;
import com.app.whosnextapp.model.PostCommentListModel;
import com.app.whosnextapp.utility.Globals;
import com.bumptech.glide.Glide;
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import im.ene.toro.ToroPlayer;
import im.ene.toro.ToroUtil;
import im.ene.toro.exoplayer.ExoPlayerViewHelper;
import im.ene.toro.helper.ToroPlayerHelper;
import im.ene.toro.media.PlaybackInfo;
import im.ene.toro.widget.Container;
import im.ene.toro.widget.PressablePlayerSelector;

public class AdsViewHolder extends RecyclerView.ViewHolder implements ToroPlayer, ToroPlayer.EventListener, View.OnClickListener {

    @BindView(R.id.btn_likes)
    AppCompatCheckBox btn_likes;
    @BindView(R.id.tv_likeCount)
    AppCompatTextView tv_likeCount;
    @BindView(R.id.tv_total_view)
    AppCompatTextView tv_total_view;
    @BindView(R.id.iv_mike)
    AppCompatImageView iv_mike;
    @BindView(R.id.tv_likes)
    AppCompatTextView tv_likes;
    @BindView(R.id.pro_image)
    CircleImageView pro_image;
    @BindView(R.id.tv_name)
    AppCompatTextView tv_name;
    @BindView(R.id.rv_comment)
    RecyclerView rv_comment;
    @BindView(R.id.tv_view_all_comments)
    AppCompatTextView tv_view_all_comments;
    @BindView(R.id.tv_addComment)
    AppCompatTextView tv_addComment;
    @BindView(R.id.Image_view)
    AppCompatImageView Image_view;
    @BindView(R.id.tv_button)
    AppCompatTextView tv_button;
    @BindView(R.id.exoplayer)
    PlayerView playerView;

    private Context context;
    private ExoPlayerAdapter.OnItemClick onItemClick;
    private GetAllDashboardData.DashboardList dashboardSingleData;
    private ViewCommentAdapter viewCommentAdapter;
    private ToroPlayerHelper toroPlayerHelper;
    private Uri mediaUri;

    AdsViewHolder(Context context, View itemView, PressablePlayerSelector selector, ExoPlayerAdapter.OnItemClick onItemClick) {
        super(itemView);
        this.onItemClick = onItemClick;
        ButterKnife.bind(this, itemView);
        this.context = context;
    }

    @NonNull
    @Override
    public View getPlayerView() {
        return playerView;
    }

    @NonNull
    @Override
    public PlaybackInfo getCurrentPlaybackInfo() {
        return toroPlayerHelper != null ? toroPlayerHelper.getLatestPlaybackInfo() : new PlaybackInfo();
    }

    @Override
    public void initialize(@NonNull Container container, @NonNull PlaybackInfo playbackInfo) {
        if (toroPlayerHelper == null) {
            toroPlayerHelper = new ExoPlayerViewHelper(this, mediaUri, new ArrayList<Uri>());
            toroPlayerHelper.addPlayerEventListener(this);
        }
        toroPlayerHelper.initialize(container, playbackInfo);
    }

    @Override
    public void play() {
        if (toroPlayerHelper != null) toroPlayerHelper.play();
    }

    @Override
    public void pause() {
        if (toroPlayerHelper != null) toroPlayerHelper.pause();
    }

    @Override
    public boolean isPlaying() {
        return toroPlayerHelper != null && toroPlayerHelper.isPlaying();
    }

    @Override
    public void release() {
        if (toroPlayerHelper != null) {
            toroPlayerHelper.removePlayerEventListener(this);
            toroPlayerHelper.release();
            toroPlayerHelper = null;
        }
    }

    @Override
    public boolean wantsToPlay() {
        return ToroUtil.visibleAreaOffset(this, itemView.getParent()) >= 0.85;
    }

    @Override
    public int getPlayerOrder() {
        return getAdapterPosition();
    }

    @Override
    public void onFirstFrameRendered() {
    }

    @Override
    public void onBuffering() {
    }

    @Override
    public void onPlaying() {
    }

    @Override
    public void onPaused() {
    }

    @Override
    public void onCompleted() {
    }

    public void bind(GetAllDashboardData.DashboardList dashboardList) {
        dashboardSingleData = dashboardList;
        if (dashboardList.getCommentList() != null) {
            if (dashboardList.getCommentList().isEmpty()) {
                tv_view_all_comments.setVisibility(View.GONE);
            } else {
                tv_view_all_comments.setVisibility(View.VISIBLE);
            }
        }

        /*Video Thumbnail*/

        if (dashboardList.getAdImageList().isEmpty()) {
            if (dashboardList.getAdVideoUrl() != null) {
                HttpProxyCacheServer proxy = Globals.getProxy(context);
                String proxyUrl = proxy.getProxyUrl(dashboardList.getAdVideoUrl());
                this.mediaUri = Uri.parse(proxyUrl);
            }
        } else {
            if (!dashboardList.getAdImageList().isEmpty())
                Glide.with(context).load(dashboardList.getAdImageList().get(0).getImageUrl()).into(Image_view);
        }
        btn_likes.setChecked(dashboardList.getIsLiked());
        tv_likeCount.setText(String.valueOf(dashboardList.getTotalLikes()));
        tv_button.setText(dashboardList.getButtonName());
        tv_total_view.setText(String.format(context.getString(R.string.text_views), dashboardList.getTotalViews()));
        Glide.with(context).load(dashboardList.getProfilePicUrl()).into(pro_image);
        String s = dashboardList.getUserName() + " " + dashboardList.getCaption();
        SpannableString ss1 = new SpannableString(s);
        ss1.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.red)), 0, dashboardList.getUserName().length(), 0);
        tv_name.setText(ss1);
        setCommentData(dashboardList.getCommentList());

        /*Click Listeners*/

        btn_likes.setOnClickListener(this);
        iv_mike.setOnClickListener(this);
        tv_likes.setOnClickListener(this);
        tv_view_all_comments.setOnClickListener(this);
        tv_addComment.setOnClickListener(this);
        pro_image.setOnClickListener(this);
        tv_name.setOnClickListener(this);
        tv_button.setOnClickListener(this);
    }

    private void setCommentData(ArrayList<PostCommentListModel.PostCommentList> commentData) {
        if (viewCommentAdapter == null) {
            viewCommentAdapter = new ViewCommentAdapter(context);
        }
        if (commentData != null) {
            // Collections.reverse(commentData);
            ArrayList<PostCommentListModel.PostCommentList> post = new ArrayList<>();
            if (commentData.size() > 2) {
                for (int i = 0; i < 2; i++) {
                    post.add(commentData.get(i));
                }
            } else {
                post.addAll(commentData);
            }
            if (rv_comment.getAdapter() == null) {
                rv_comment.setHasFixedSize(false);
                LinearLayoutManager layoutManager = new LinearLayoutManager(context);
                rv_comment.setLayoutManager(layoutManager);
                rv_comment.setAdapter(viewCommentAdapter);
            }
            viewCommentAdapter.doRefresh(post);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_likes:
                if (onItemClick != null)
                    onItemClick.onClickLike(dashboardSingleData, getAdapterPosition() - 1);
                break;
            case R.id.iv_mike:
                if (onItemClick != null)
                    onItemClick.displayComment(getAdapterPosition() - 1);
                break;

            case R.id.tv_likes:
                if (onItemClick != null)
                    onItemClick.likeDisplay(getAdapterPosition() - 1);
                break;
            case R.id.tv_view_all_comments:
                if (onItemClick != null)
                    onItemClick.displayAllComments(getAdapterPosition() - 1);
                break;
            case R.id.tv_addComment:
                if (onItemClick != null)
                    onItemClick.onClickAddComment(getAdapterPosition() - 1);
                break;
            case R.id.tv_name:
                if (onItemClick != null)
                    onItemClick.onClickSmallUsername(getAdapterPosition() - 1);
                break;
            case R.id.pro_image:
                if (onItemClick != null)
                    onItemClick.onClickSmallProfile(getAdapterPosition() - 1);
                break;
            case R.id.tv_button:
                onItemClick.onClickButton(getAdapterPosition());
                break;
        }
    }
}
