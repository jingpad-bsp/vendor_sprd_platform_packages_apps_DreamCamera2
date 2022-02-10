package com.android.camera.data;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.android.camera.data.FilmstripItemAttributes.Attributes;
import com.android.camera.debug.Log;
import com.android.camera2.R;
import com.google.common.base.Optional;

import javax.annotation.Nonnull;
public class PhotoItem3D extends PhotoItem {
    /* SPRD: fix bug603131 3D photo should not support share, edit, puzzle @{ */
    private static final FilmstripItemAttributes PHOTO_ITEM_3D_ATTRIBUTES =
            new FilmstripItemAttributes.Builder()
                    .with(Attributes.CAN_DELETE)
                    .with(Attributes.CAN_SWIPE_AWAY)
                    .with(Attributes.CAN_ZOOM_IN_PLACE)
                    .with(Attributes.HAS_DETAILED_CAPTURE_INFO)
                    .with(Attributes.IS_IMAGE)
                    .build();
    /* @} */
    private static final Log.Tag TAG = new Log.Tag("3DPhotoItem");

    public PhotoItem3D(Context context, PhotoItemData data,
                       PhotoItemFactory photoItemFactory) {
        super(context, data, photoItemFactory, PHOTO_ITEM_3D_ATTRIBUTES);
    }

    @Override
    public View getView(Optional<View> optionalView, LocalFilmstripDataAdapter adapter,
                        boolean isInProgress, VideoClickedCallback videoClickedCallback) {
        View view;
        if (!optionalView.isPresent()) {
            view = LayoutInflater.from(mContext).inflate(R.layout.filmstrip_photo_3d, null);
            view.setTag(R.id.mediadata_tag_viewtype, getItemViewType().ordinal());
            view.setTag(R.id.mediadata_tag_target_3dphoto, null);
            return view;
        }
        return optionalView.get();

    }

    @Override
    public void renderThumbnail(@Nonnull View view) {
        if (view == null) return;
        super.renderThumbnail(view.findViewById(R.id.td_photo_item_view));
    }

    @Override
    public FilmstripItemType getItemViewType() {
        return FilmstripItemType.TDPHOTO;
    }

}