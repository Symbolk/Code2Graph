package com.mayank.models;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.ViewDataBinding;

/**
 *
 */

class BaseViewModel<B extends ViewDataBinding> extends BaseObservable {
    private B binding;
    private Context context;

    BaseViewModel(Context context, B binding) {
        this.context = context;
        this.binding = binding;
    }

    BaseViewModel() {
    }

    B getBinding() {
        return binding;
    }

    public Context getContext() {
        return context;
    }
}
