package com.dvlab.photogallery.ui;

import android.support.v4.app.Fragment;

public class PhotoPageActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new PhotoPageFragment();
    }
}
