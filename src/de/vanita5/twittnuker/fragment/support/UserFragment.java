package de.vanita5.twittnuker.fragment.support;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.mariotaku.menucomponent.internal.menu.MenuUtils;
import org.mariotaku.querybuilder.Expression;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.iface.IThemedActivity;
import de.vanita5.twittnuker.activity.support.AccountSelectorActivity;
import de.vanita5.twittnuker.activity.support.ColorPickerDialogActivity;
import de.vanita5.twittnuker.activity.support.LinkHandlerActivity;
import de.vanita5.twittnuker.activity.support.UserListSelectorActivity;
import de.vanita5.twittnuker.activity.support.UserProfileEditorActivity;
import de.vanita5.twittnuker.adapter.support.SupportTabsAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.fragment.iface.IBaseFragment.SystemWindowsInsetsCallback;
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback;
import de.vanita5.twittnuker.graphic.ActionBarColorDrawable;
import de.vanita5.twittnuker.loader.support.ParcelableUserLoader;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.ParcelableUserList;
import de.vanita5.twittnuker.model.SingleResponse;
import de.vanita5.twittnuker.provider.TweetStore.CachedUsers;
import de.vanita5.twittnuker.provider.TweetStore.Filters;
import de.vanita5.twittnuker.text.TextAlphaSpan;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.ContentValuesCreator;
import de.vanita5.twittnuker.util.ImageLoaderWrapper;
import de.vanita5.twittnuker.util.MathUtils;
import de.vanita5.twittnuker.util.ParseUtils;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.TwidereLinkify;
import de.vanita5.twittnuker.util.TwidereLinkify.OnLinkClickListener;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.menu.TwidereMenuInfo;
import de.vanita5.twittnuker.util.message.FriendshipUpdatedEvent;
import de.vanita5.twittnuker.util.message.ProfileUpdatedEvent;
import de.vanita5.twittnuker.util.message.TaskStateChangedEvent;
import de.vanita5.twittnuker.view.HeaderDrawerLayout;
import de.vanita5.twittnuker.view.HeaderDrawerLayout.DrawerCallback;
import de.vanita5.twittnuker.view.ProfileBannerImageView;
import de.vanita5.twittnuker.view.ProfileImageView;
import de.vanita5.twittnuker.view.TabPagerIndicator;
import de.vanita5.twittnuker.view.TintedStatusFrameLayout;
import de.vanita5.twittnuker.view.iface.IColorLabelView;
import de.vanita5.twittnuker.view.iface.IExtendedView.OnSizeChangedListener;

import java.util.Locale;

import twitter4j.Relationship;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import static android.text.TextUtils.isEmpty;
import static de.vanita5.twittnuker.util.ParseUtils.parseLong;
import static de.vanita5.twittnuker.util.UserColorUtils.clearUserColor;
import static de.vanita5.twittnuker.util.UserColorUtils.getUserColor;
import static de.vanita5.twittnuker.util.UserColorUtils.setUserColor;
import static de.vanita5.twittnuker.util.Utils.formatToLongTimeString;
import static de.vanita5.twittnuker.util.Utils.getAccountColor;
import static de.vanita5.twittnuker.util.Utils.getAccountScreenName;
import static de.vanita5.twittnuker.util.Utils.getDisplayName;
import static de.vanita5.twittnuker.util.Utils.getErrorMessage;
import static de.vanita5.twittnuker.util.Utils.getLocalizedNumber;
import static de.vanita5.twittnuker.util.Utils.getOriginalTwitterProfileImage;
import static de.vanita5.twittnuker.util.Utils.getTwitterInstance;
import static de.vanita5.twittnuker.util.Utils.getUserTypeIconRes;
import static de.vanita5.twittnuker.util.Utils.openImage;
import static de.vanita5.twittnuker.util.Utils.openStatus;
import static de.vanita5.twittnuker.util.Utils.openTweetSearch;
import static de.vanita5.twittnuker.util.Utils.openUserFollowers;
import static de.vanita5.twittnuker.util.Utils.openUserFriends;
import static de.vanita5.twittnuker.util.Utils.openUserProfile;
import static de.vanita5.twittnuker.util.Utils.openUserTimeline;
import static de.vanita5.twittnuker.util.Utils.setMenuItemAvailability;
import static de.vanita5.twittnuker.util.Utils.showInfoMessage;

public class UserFragment extends BaseSupportFragment implements OnClickListener,
		OnMenuItemClickListener, OnLinkClickListener, OnSizeChangedListener,
        OnSharedPreferenceChangeListener, OnTouchListener, DrawerCallback, SupportFragmentCallback,
        SystemWindowsInsetsCallback {

	public static final String TRANSITION_NAME_PROFILE_IMAGE = "profile_image";
	public static final String TRANSITION_NAME_PROFILE_TYPE = "profile_type";
    public static final String TRANSITION_NAME_CARD = "card";

	private static final int LOADER_ID_USER = 1;
	private static final int LOADER_ID_FRIENDSHIP = 2;

	private ImageLoaderWrapper mProfileImageLoader;

    private ProfileImageView mProfileImageView;
	private ImageView mProfileTypeView;
	private ProfileBannerImageView mProfileBannerView;
	private TextView mNameView, mScreenNameView, mDescriptionView, mLocationView, mURLView, mCreatedAtView,
			mTweetCount, mFollowersCount, mFriendsCount, mErrorMessageView;
	private View mDescriptionContainer, mLocationContainer, mURLContainer, mTweetsContainer, mFollowersContainer,
			mFriendsContainer;
	private Button mRetryButton;
    private IColorLabelView mProfileNameContainer;
	private View mProgressContainer, mErrorRetryContainer;
    private View mCardContent;
	private View mProfileBannerSpace;
    private TintedStatusFrameLayout mTintedStatusContent;
    private HeaderDrawerLayout mHeaderDrawerLayout;
	private ViewPager mViewPager;
    private TabPagerIndicator mPagerIndicator;
    private CardView mCardView;
    private View mProfileBannerContainer;
    private Button mFollowButton;
    private ProgressBar mFollowProgress;

	private SupportTabsAdapter mPagerAdapter;

	private Relationship mRelationship;
	private ParcelableUser mUser = null;

    private Locale mLocale;
    private boolean mGetUserInfoLoaderInitialized, mGetFriendShipLoaderInitialized;
    private int mBannerWidth;
    private ActionBarDrawable mActionBarBackground;
    private Fragment mCurrentVisibleFragment;


    @Subscribe
    public void notifyTaskStateChanged(TaskStateChangedEvent event) {
        updateRefreshState();
    }

    @Subscribe
    public void notifyFriendshipUpdated(FriendshipUpdatedEvent event) {
        if (!event.user.equals(mUser)) return;
        getFriendship();
    }

    private void updateRefreshState() {
        final ParcelableUser user = mUser;
        if (user == null) return;
        final AsyncTwitterWrapper twitter = getTwitterWrapper();
        final boolean is_creating_friendship = twitter != null
                && twitter.isCreatingFriendship(user.account_id, user.id);
        final boolean is_destroying_friendship = twitter != null
                && twitter.isDestroyingFriendship(user.account_id, user.id);
        setProgressBarIndeterminateVisibility(is_creating_friendship || is_destroying_friendship);
        invalidateOptionsMenu();
    }

	private final LoaderCallbacks<SingleResponse<ParcelableUser>> mUserInfoLoaderCallbacks = new LoaderCallbacks<SingleResponse<ParcelableUser>>() {

		@Override
		public Loader<SingleResponse<ParcelableUser>> onCreateLoader(final int id, final Bundle args) {
            final boolean omitIntentExtra = args.getBoolean(EXTRA_OMIT_INTENT_EXTRA, true);
            final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
            final long userId = args.getLong(EXTRA_USER_ID, -1);
            final String screenName = args.getString(EXTRA_SCREEN_NAME);
            if (mUser == null && (!omitIntentExtra || !args.containsKey(EXTRA_USER))) {
                mCardContent.setVisibility(View.GONE);
				mErrorRetryContainer.setVisibility(View.GONE);
				mProgressContainer.setVisibility(View.VISIBLE);
				mErrorMessageView.setText(null);
				mErrorMessageView.setVisibility(View.GONE);
				setListShown(false);
			}
			setProgressBarIndeterminateVisibility(true);
			final ParcelableUser user = mUser;
			return new ParcelableUserLoader(getActivity(), accountId, userId, screenName, getArguments(),
					omitIntentExtra, user == null || !user.is_cache && userId != user.id);
		}

		@Override
		public void onLoaderReset(final Loader<SingleResponse<ParcelableUser>> loader) {

		}

		@Override
		public void onLoadFinished(final Loader<SingleResponse<ParcelableUser>> loader,
								   final SingleResponse<ParcelableUser> data) {
			if (getActivity() == null) return;
			if (data.getData() != null && data.getData().id > 0) {
				final ParcelableUser user = data.getData();
                mCardContent.setVisibility(View.VISIBLE);
				mErrorRetryContainer.setVisibility(View.GONE);
				mProgressContainer.setVisibility(View.GONE);
				setListShown(true);
				displayUser(user);
				if (user.is_cache) {
					final Bundle args = new Bundle();
					args.putLong(EXTRA_ACCOUNT_ID, user.account_id);
					args.putLong(EXTRA_USER_ID, user.id);
					args.putString(EXTRA_SCREEN_NAME, user.screen_name);
					args.putBoolean(EXTRA_OMIT_INTENT_EXTRA, true);
					getLoaderManager().restartLoader(LOADER_ID_USER, args, this);
				}
			} else if (mUser != null && mUser.is_cache) {
                mCardContent.setVisibility(View.VISIBLE);
				mErrorRetryContainer.setVisibility(View.GONE);
				mProgressContainer.setVisibility(View.GONE);
				setListShown(true);
				displayUser(mUser);
			} else {
				if (data.hasException()) {
					mErrorMessageView.setText(getErrorMessage(getActivity(), data.getException()));
					mErrorMessageView.setVisibility(View.VISIBLE);
				}
                mCardContent.setVisibility(View.GONE);
				mErrorRetryContainer.setVisibility(View.VISIBLE);
				mProgressContainer.setVisibility(View.GONE);
			}
			setProgressBarIndeterminateVisibility(false);
		}

	};
	private final LoaderCallbacks<SingleResponse<Relationship>> mFriendshipLoaderCallbacks = new LoaderCallbacks<SingleResponse<Relationship>>() {

		@Override
		public Loader<SingleResponse<Relationship>> onCreateLoader(final int id, final Bundle args) {
			invalidateOptionsMenu();
            mFollowButton.setVisibility(View.GONE);
            mFollowProgress.setVisibility(View.VISIBLE);
			final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
			final long userId = args.getLong(EXTRA_USER_ID, -1);
            return new RelationshipLoader(getActivity(), accountId, userId);
		}

		@Override
		public void onLoaderReset(final Loader<SingleResponse<Relationship>> loader) {

		}

		@Override
		public void onLoadFinished(final Loader<SingleResponse<Relationship>> loader,
								   final SingleResponse<Relationship> data) {
            mFollowProgress.setVisibility(View.GONE);
			final ParcelableUser user = mUser;
            final Relationship relationship = data.getData();
            mRelationship = relationship;
			if (user == null) return;
			invalidateOptionsMenu();
            final boolean isMyself = user.account_id == user.id;
            if (isMyself) {
                mFollowButton.setText(R.string.edit);
                mFollowButton.setVisibility(View.VISIBLE);
            } else if (relationship != null) {
                if (relationship.isSourceBlockingTarget()) {
                    mFollowButton.setText(R.string.unblock);
                } else if (relationship.isSourceFollowingTarget()) {
                    mFollowButton.setText(R.string.unfollow);
                } else {
                    mFollowButton.setText(R.string.follow);
                }
				final ContentResolver resolver = getContentResolver();
                final String where = Expression.equals(CachedUsers.USER_ID, user.id).getSQL();
				resolver.delete(CachedUsers.CONTENT_URI, where, null);
				// I bet you don't want to see blocked user in your auto
				// complete list.
				if (!data.getData().isSourceBlockingTarget()) {
					final ContentValues cachedValues = ParcelableUser.makeCachedUserContentValues(user);
					if (cachedValues != null) {
						resolver.insert(CachedUsers.CONTENT_URI, cachedValues);
					}
				}
                mFollowButton.setVisibility(View.VISIBLE);
			} else {
                mFollowButton.setText(null);
                mFollowButton.setVisibility(View.GONE);
//                mFollowingYouIndicator.setVisibility(View.GONE);
			}
		}

	};

	public void displayUser(final ParcelableUser user) {
        mUser = user;
        final FragmentActivity activity = getActivity();
        if (user == null || user.id <= 0 || activity == null) return;
		final Resources res = getResources();
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_USER);
		lm.destroyLoader(LOADER_ID_FRIENDSHIP);
		final boolean userIsMe = user.account_id == user.id;
        mCardContent.setVisibility(View.VISIBLE);
		mErrorRetryContainer.setVisibility(View.GONE);
		mProgressContainer.setVisibility(View.GONE);
		mUser = user;
        final int userColor = getUserColor(activity, user.id, true);
        mProfileImageView.setBorderColor(userColor != 0 ? userColor : Color.WHITE);
        mProfileNameContainer.drawEnd(getAccountColor(activity, user.account_id));
		mNameView.setText(user.name);
		final int typeIconRes = getUserTypeIconRes(user.is_verified, user.is_protected);
		if (typeIconRes != 0) {
			mProfileTypeView.setImageResource(typeIconRes);
			mProfileTypeView.setVisibility(View.VISIBLE);
		} else {
			mProfileTypeView.setImageDrawable(null);
			mProfileTypeView.setVisibility(View.GONE);
		}
		mScreenNameView.setText("@" + user.screen_name);
		mDescriptionContainer.setVisibility(userIsMe || !isEmpty(user.description_html) ? View.VISIBLE : View.GONE);
		mDescriptionView.setText(user.description_html != null ? Html.fromHtml(user.description_html) : null);
		final TwidereLinkify linkify = new TwidereLinkify(this);
        linkify.setLinkTextColor(user.link_color);
		linkify.applyAllLinks(mDescriptionView, user.account_id, false);
		mDescriptionView.setMovementMethod(null);
		mLocationContainer.setVisibility(userIsMe || !isEmpty(user.location) ? View.VISIBLE : View.GONE);
		mLocationView.setText(user.location);
		mURLContainer.setVisibility(userIsMe || !isEmpty(user.url) || !isEmpty(user.url_expanded) ? View.VISIBLE
				: View.GONE);
		mURLView.setText(isEmpty(user.url_expanded) ? user.url : user.url_expanded);
        mURLView.setLinkTextColor(user.link_color);
		mURLView.setMovementMethod(null);
        final String createdAt = formatToLongTimeString(activity, user.created_at);
		final float daysSinceCreated = (System.currentTimeMillis() - user.created_at) / 1000 / 60 / 60 / 24;
		final int dailyTweets = Math.round(user.statuses_count / Math.max(1, daysSinceCreated));
		mCreatedAtView.setText(res.getQuantityString(R.plurals.created_at_with_N_tweets_per_day, dailyTweets,
				createdAt, dailyTweets));
		mTweetCount.setText(getLocalizedNumber(mLocale, user.statuses_count));
		mFollowersCount.setText(getLocalizedNumber(mLocale, user.followers_count));
		mFriendsCount.setText(getLocalizedNumber(mLocale, user.friends_count));
		if (userColor != 0) {
			mProfileImageLoader.displayProfileImage(mProfileImageView,
					getOriginalTwitterProfileImage(user.profile_image_url));
			setupUserColorActionBar(userColor);
		} else {
			mProfileImageLoader.displayProfileImage(mProfileImageView,
                    getOriginalTwitterProfileImage(user.profile_image_url));
            setupUserColorActionBar(user.link_color);
		}
		final int defWidth = res.getDisplayMetrics().widthPixels;
		final int width = mBannerWidth > 0 ? mBannerWidth : defWidth;
		mProfileImageLoader.displayProfileBanner(mProfileBannerView, user.profile_banner_url, width);
        final Relationship relationship = mRelationship;
        if (relationship == null || relationship.getTargetUserId() != user.id) {
			getFriendship();
		}
        activity.setTitle(getDisplayName(user));
        updateTitleColor();
		invalidateOptionsMenu();
	}

    @Override
    public void fling(float velocity) {
        final Fragment fragment = mCurrentVisibleFragment;
        if (fragment instanceof DrawerCallback) {
            ((DrawerCallback) fragment).fling(velocity);
        }
    }

    @Override
    public void scrollBy(float dy) {
        final Fragment fragment = mCurrentVisibleFragment;
        if (fragment instanceof DrawerCallback) {
            ((DrawerCallback) fragment).scrollBy(dy);
        }
    }

    @Override
    public boolean canScroll(float dy) {
        final Fragment fragment = mCurrentVisibleFragment;
        return fragment instanceof DrawerCallback && ((DrawerCallback) fragment).canScroll(dy);
    }

    @Override
    public boolean isScrollContent(float x, float y) {
        final ViewPager v = mViewPager;
        final int[] location = new int[2];
        v.getLocationOnScreen(location);
        return x >= location[0] && x <= location[0] + v.getWidth()
                && y >= location[1] && y <= location[1] + v.getHeight();
    }

    @Override
    public void cancelTouch() {
        final Fragment fragment = mCurrentVisibleFragment;
        if (fragment instanceof DrawerCallback) {
            ((DrawerCallback) fragment).cancelTouch();
        }
    }

    @Override
    public void topChanged(int top) {
        final HeaderDrawerLayout drawer = mHeaderDrawerLayout;
        if (drawer == null) return;
        final int offset = drawer.getPaddingTop() - top;
        updateScrollOffset(offset);

        final Fragment fragment = mCurrentVisibleFragment;
        if (fragment instanceof DrawerCallback) {
            ((DrawerCallback) fragment).topChanged(top);
        }
    }

    @Override
    public boolean shouldLayoutHeaderBottom() {
        final HeaderDrawerLayout drawer = mHeaderDrawerLayout;
        final CardView card = mCardView;
        if (drawer == null || card == null) return false;
        return card.getTop() + drawer.getHeaderTop() - drawer.getPaddingTop() <= 0;
    }

    @Override
    public Fragment getCurrentVisibleFragment() {
        return mCurrentVisibleFragment;
    }

    @Override
    public void onDetachFragment(Fragment fragment) {

    }

    @Override
    public void onSetUserVisibleHint(Fragment fragment, boolean isVisibleToUser) {
        mCurrentVisibleFragment = isVisibleToUser ? fragment : null;
    }

    @Override
    public boolean triggerRefresh(int position) {
        return false;
    }

    @Override
    public boolean getSystemWindowsInsets(Rect insets) {
        return false;
    }

	public void getUserInfo(final long accountId, final long userId, final String screenName,
							final boolean omitIntentExtra) {
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_USER);
		lm.destroyLoader(LOADER_ID_FRIENDSHIP);
		final Bundle args = new Bundle();
		args.putLong(EXTRA_ACCOUNT_ID, accountId);
		args.putLong(EXTRA_USER_ID, userId);
		args.putString(EXTRA_SCREEN_NAME, screenName);
		args.putBoolean(EXTRA_OMIT_INTENT_EXTRA, omitIntentExtra);
		if (!mGetUserInfoLoaderInitialized) {
			lm.initLoader(LOADER_ID_USER, args, mUserInfoLoaderCallbacks);
			mGetUserInfoLoaderInitialized = true;
		} else {
			lm.restartLoader(LOADER_ID_USER, args, mUserInfoLoaderCallbacks);
		}
		if (accountId == -1 || userId == -1 && screenName == null) {
            mCardContent.setVisibility(View.GONE);
			mErrorRetryContainer.setVisibility(View.GONE);
		}
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		final ParcelableUser user = mUser;
		switch (requestCode) {
			case REQUEST_SET_COLOR: {
				if (user == null) return;
				if (resultCode == Activity.RESULT_OK) {
					if (data == null) return;
					final int color = data.getIntExtra(EXTRA_COLOR, Color.TRANSPARENT);
					setUserColor(getActivity(), mUser.id, color);
				} else if (resultCode == ColorPickerDialogActivity.RESULT_CLEARED) {
					clearUserColor(getActivity(), mUser.id);
				}
				break;
			}
			case REQUEST_ADD_TO_LIST: {
				if (user == null) return;
				if (resultCode == Activity.RESULT_OK && data != null) {
					final AsyncTwitterWrapper twitter = getTwitterWrapper();
					final ParcelableUserList list = data.getParcelableExtra(EXTRA_USER_LIST);
					if (list == null || twitter == null) return;
					twitter.addUserListMembersAsync(user.account_id, list.id, user);
				}
				break;
			}
			case REQUEST_SELECT_ACCOUNT: {
				if (user == null) return;
				if (resultCode == Activity.RESULT_OK) {
					if (data == null || !data.hasExtra(EXTRA_ID)) return;
					final long accountId = data.getLongExtra(EXTRA_ID, -1);
                    openUserProfile(getActivity(), accountId, user.id, user.screen_name, null);
				}
				break;
			}
		}

	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mTintedStatusContent = (TintedStatusFrameLayout) activity.findViewById(R.id.main_content);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        getSharedPreferences(USER_COLOR_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this);
        mLocale = getResources().getConfiguration().locale;
        final Bundle args = getArguments();
        long accountId = -1, userId = -1;
        String screenName = null;
        if (savedInstanceState != null) {
            args.putAll(savedInstanceState);
        } else {
            accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
            userId = args.getLong(EXTRA_USER_ID, -1);
            screenName = args.getString(EXTRA_SCREEN_NAME);
        }
        mProfileImageLoader = getApplication().getImageLoaderWrapper();
        final FragmentActivity activity = getActivity();

        ViewCompat.setTransitionName(mProfileImageView, TRANSITION_NAME_PROFILE_IMAGE);
        ViewCompat.setTransitionName(mProfileTypeView, TRANSITION_NAME_PROFILE_TYPE);
        ViewCompat.setTransitionName(mCardView, TRANSITION_NAME_CARD);

        mHeaderDrawerLayout.setDrawerCallback(this);

        mPagerAdapter = new SupportTabsAdapter(activity, getChildFragmentManager());

        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mPagerAdapter);
        mPagerIndicator.setViewPager(mViewPager);
        mPagerIndicator.setTabDisplayOption(TabPagerIndicator.LABEL);

        mFollowButton.setOnClickListener(this);
        mProfileImageView.setOnClickListener(this);
        mProfileBannerView.setOnClickListener(this);
		mTweetsContainer.setOnClickListener(this);
        mFollowersContainer.setOnClickListener(this);
        mFriendsContainer.setOnClickListener(this);
        mRetryButton.setOnClickListener(this);
        mProfileBannerView.setOnSizeChangedListener(this);
        mProfileBannerSpace.setOnTouchListener(this);

        getUserInfo(accountId, userId, screenName, false);

        setupBaseActionBar();

        setupUserPages();
    }

    @Override
    public void onStart() {
        super.onStart();
        final Bus bus = TwittnukerApplication.getInstance(getActivity()).getMessageBus();
        bus.register(this);
    }


    @Subscribe
    public void notifyProfileUpdated(ProfileUpdatedEvent event) {
        final ParcelableUser user = mUser;
        if (user == null || !user.equals(event.user)) return;
        displayUser(event.user);
    }

    @Override
    public void onStop() {
        final Bus bus = TwittnukerApplication.getInstance(getActivity()).getMessageBus();
        bus.unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putParcelable(EXTRA_USER, mUser);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        mUser = null;
        mRelationship = null;
        final LoaderManager lm = getLoaderManager();
        lm.destroyLoader(LOADER_ID_USER);
        lm.destroyLoader(LOADER_ID_FRIENDSHIP);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        if (!shouldUseNativeMenu()) return;
        inflater.inflate(R.menu.menu_user_profile, menu);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        if (!shouldUseNativeMenu() || !menu.hasVisibleItems()) return;
        setMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        return handleMenuItemClick(item);
    }

    @Override
	public void onClick(final View view) {
		final FragmentActivity activity = getActivity();
		final ParcelableUser user = mUser;
		if (activity == null || user == null) return;
		switch (view.getId()) {
			case R.id.retry: {
				getUserInfo(true);
				break;
			}
            case R.id.follow: {
                if (user.id == user.account_id) {
                    final Bundle extras = new Bundle();
                    extras.putLong(EXTRA_ACCOUNT_ID, user.account_id);
                    final Intent intent = new Intent(INTENT_ACTION_EDIT_USER_PROFILE);
                    intent.setClass(getActivity(), UserProfileEditorActivity.class);
                    intent.putExtras(extras);
                    startActivity(intent);
                    break;
                }
                final Relationship relationship = mRelationship;
                final AsyncTwitterWrapper twitter = getTwitterWrapper();
                if (relationship == null || twitter == null) return;
                if (relationship.isSourceBlockingTarget()) {
                    twitter.destroyBlockAsync(user.account_id, user.id);
                } else if (relationship.isSourceFollowingTarget()) {
                    DestroyFriendshipDialogFragment.show(getFragmentManager(), user);
                } else {
                    twitter.createFriendshipAsync(user.account_id, user.id);
                }
                break;
            }
			case R.id.profile_image: {
				final String profile_image_url_string = getOriginalTwitterProfileImage(mUser.profile_image_url);
				openImage(activity, user.account_id, profile_image_url_string, false);
				break;
			}
			case R.id.profile_banner: {
				final String profile_banner_url = mUser.profile_banner_url;
				if (profile_banner_url == null) return;
				openImage(getActivity(), user.account_id, profile_banner_url + "/ipad_retina", false);
				break;
			}
			case R.id.tweets_container: {
				openUserTimeline(getActivity(), user.account_id, user.id, user.screen_name);
				break;
			}
			case R.id.followers_container: {
				openUserFollowers(getActivity(), user.account_id, user.id, user.screen_name);
				break;
			}
			case R.id.friends_container: {
				openUserFriends(getActivity(), user.account_id, user.id, user.screen_name);
				break;
			}
			case R.id.name_container: {
				if (user.account_id != user.id) return;
				startActivity(new Intent(getActivity(), UserProfileEditorActivity.class));
				break;
			}
		}

	}

	@Override
	public void onLinkClick(final String link, final String orig, final long account_id, final int type,
							final boolean sensitive) {
		final ParcelableUser user = mUser;
		if (user == null) return;
		switch (type) {
			case TwidereLinkify.LINK_TYPE_MENTION: {
				openUserProfile(getActivity(), user.account_id, -1, link, null);
				break;
			}
			case TwidereLinkify.LINK_TYPE_HASHTAG: {
				openTweetSearch(getActivity(), user.account_id, "#" + link);
				break;
			}
			case TwidereLinkify.LINK_TYPE_LINK: {
				final Uri uri = Uri.parse(link);
				final Intent intent;
				if (uri.getScheme() != null) {
					intent = new Intent(Intent.ACTION_VIEW, uri);
				} else {
					intent = new Intent(Intent.ACTION_VIEW, uri.buildUpon().scheme("http").build());
				}
				startActivity(intent);
				break;
			}
			case TwidereLinkify.LINK_TYPE_LIST: {
				if (link == null) break;
				final String[] mentionList = link.split("/");
				if (mentionList.length != 2) {
					break;
				}
				break;
			}
			case TwidereLinkify.LINK_TYPE_STATUS: {
				openStatus(getActivity(), account_id, parseLong(link));
				break;
			}
		}
	}

	@Override
	public boolean onMenuItemClick(final MenuItem item) {
		return handleMenuItemClick(item);

	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
		if (mUser == null || !ParseUtils.parseString(mUser.id).equals(key)) return;
		displayUser(mUser);
	}

	@Override
	public void onSizeChanged(final View view, final int w, final int h, final int oldw, final int oldh) {
		mBannerWidth = w;
		if (w != oldw || h != oldh) {
			requestFitSystemWindows();
		}
	}

	@Override
	public boolean onTouch(final View v, final MotionEvent event) {
		return mProfileBannerView.dispatchTouchEvent(event);
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
        mHeaderDrawerLayout = (HeaderDrawerLayout) view.findViewById(R.id.user_profile_drawer);
        final View headerView = mHeaderDrawerLayout.getHeader();
        final View contentView = mHeaderDrawerLayout.getContent();
        super.onViewCreated(view, savedInstanceState);
        mCardContent = headerView.findViewById(R.id.card_content);
        mErrorRetryContainer = headerView.findViewById(R.id.error_retry_container);
        mProgressContainer = headerView.findViewById(R.id.progress_container);
        mRetryButton = (Button) headerView.findViewById(R.id.retry);
        mErrorMessageView = (TextView) headerView.findViewById(R.id.error_message);
        mProfileBannerView = (ProfileBannerImageView) view.findViewById(R.id.profile_banner);
        mProfileBannerContainer = view.findViewById(R.id.profile_banner_container);
        mCardView = (CardView) headerView.findViewById(R.id.card);
		mNameView = (TextView) headerView.findViewById(R.id.name);
		mScreenNameView = (TextView) headerView.findViewById(R.id.screen_name);
		mDescriptionView = (TextView) headerView.findViewById(R.id.description);
		mLocationView = (TextView) headerView.findViewById(R.id.location);
		mURLView = (TextView) headerView.findViewById(R.id.url);
		mCreatedAtView = (TextView) headerView.findViewById(R.id.created_at);
		mTweetsContainer = headerView.findViewById(R.id.tweets_container);
		mTweetCount = (TextView) headerView.findViewById(R.id.statuses_count);
		mFollowersContainer = headerView.findViewById(R.id.followers_container);
		mFollowersCount = (TextView) headerView.findViewById(R.id.followers_count);
		mFriendsContainer = headerView.findViewById(R.id.friends_container);
		mFriendsCount = (TextView) headerView.findViewById(R.id.friends_count);
        mProfileNameContainer = (IColorLabelView) headerView.findViewById(R.id.profile_name_container);
        mProfileImageView = (ProfileImageView) headerView.findViewById(R.id.profile_image);
		mProfileTypeView = (ImageView) headerView.findViewById(R.id.profile_type);
		mDescriptionContainer = headerView.findViewById(R.id.description_container);
		mLocationContainer = headerView.findViewById(R.id.location_container);
		mURLContainer = headerView.findViewById(R.id.url_container);
		mProfileBannerSpace = headerView.findViewById(R.id.profile_banner_space);
		mViewPager = (ViewPager) contentView.findViewById(R.id.view_pager);
        mPagerIndicator = (TabPagerIndicator) contentView.findViewById(R.id.view_pager_tabs);
        mFollowButton = (Button) headerView.findViewById(R.id.follow);
        mFollowProgress = (ProgressBar) headerView.findViewById(R.id.follow_progress);
	}

    @Override
    protected void fitSystemWindows(Rect insets) {
        super.fitSystemWindows(insets);
        mHeaderDrawerLayout.setPadding(insets.left, insets.top, insets.right, insets.bottom);
        final FragmentActivity activity = getActivity();
        final boolean isTransparentBackground;
        if (activity instanceof IThemedActivity) {
            final int themeRes = ((IThemedActivity) activity).getCurrentThemeResourceId();
            isTransparentBackground = ThemeUtils.isTransparentBackground(themeRes);
        } else {
            isTransparentBackground = ThemeUtils.isTransparentBackground(getActivity());
        }
        mHeaderDrawerLayout.setClipToPadding(isTransparentBackground);
    }

    public void setListShown(boolean shown) {
        final TintedStatusFrameLayout tintedStatus = mTintedStatusContent;
        if (tintedStatus == null) return;
        final FragmentActivity activity = getActivity();
        final LinkHandlerActivity linkHandler = (LinkHandlerActivity) activity;
        final boolean drawColor = !ThemeUtils.isDarkTheme(linkHandler.getCurrentThemeResourceId());
        tintedStatus.setDrawShadow(shown);
        tintedStatus.setDrawColor(drawColor);
    }

	private void getFriendship() {
        mRelationship = null;
		final ParcelableUser user = mUser;
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_FRIENDSHIP);
		final Bundle args = new Bundle();
		args.putLong(EXTRA_ACCOUNT_ID, user.account_id);
		args.putLong(EXTRA_USER_ID, user.id);
		if (!mGetFriendShipLoaderInitialized) {
			lm.initLoader(LOADER_ID_FRIENDSHIP, args, mFriendshipLoaderCallbacks);
			mGetFriendShipLoaderInitialized = true;
		} else {
			lm.restartLoader(LOADER_ID_FRIENDSHIP, args, mFriendshipLoaderCallbacks);
		}
	}

	private void getUserInfo(final boolean omitIntentExtra) {
		final ParcelableUser user = mUser;
		if (user == null) return;
		getUserInfo(user.account_id, user.id, user.screen_name, omitIntentExtra);
	}

	private boolean handleMenuItemClick(final MenuItem item) {
		final AsyncTwitterWrapper twitter = getTwitterWrapper();
		final ParcelableUser user = mUser;
		final Relationship relationship = mRelationship;
		if (user == null || twitter == null) return false;
		switch (item.getItemId()) {
			case MENU_BLOCK: {
				if (mRelationship != null) {
					if (mRelationship.isSourceBlockingTarget()) {
						twitter.destroyBlockAsync(user.account_id, user.id);
					} else {
						CreateUserBlockDialogFragment.show(getFragmentManager(), user);
					}
				}
				break;
			}
			case MENU_REPORT_SPAM: {
				ReportSpamDialogFragment.show(getFragmentManager(), user);
				break;
			}
			case MENU_ADD_TO_FILTER: {
				final boolean filtering = Utils.isFilteringUser(getActivity(), user.id);
				final ContentResolver cr = getContentResolver();
				if (filtering) {
                    final Expression where = Expression.equals(Filters.Users.USER_ID, user.id);
					cr.delete(Filters.Users.CONTENT_URI, where.getSQL(), null);
					showInfoMessage(getActivity(), R.string.message_user_unmuted, false);
				} else {
					cr.insert(Filters.Users.CONTENT_URI, ContentValuesCreator.makeFilteredUserContentValues(user));
					showInfoMessage(getActivity(), R.string.message_user_muted, false);
				}
				break;
			}
			case MENU_MUTE_USER: {
				if (mRelationship != null) {
					if (mRelationship.isSourceMutingTarget()) {
						twitter.destroyMuteAsync(user.account_id, user.id);
					} else {
						CreateUserMuteDialogFragment.show(getFragmentManager(), user);
					}
				}
				break;
			}
			case MENU_MENTION: {
				final Intent intent = new Intent(INTENT_ACTION_MENTION);
				final Bundle bundle = new Bundle();
				bundle.putParcelable(EXTRA_USER, user);
				intent.putExtras(bundle);
				startActivity(intent);
				break;
			}
			case MENU_SEND_DIRECT_MESSAGE: {
				final Uri.Builder builder = new Uri.Builder();
				builder.scheme(SCHEME_TWITTNUKER);
				builder.authority(AUTHORITY_DIRECT_MESSAGES_CONVERSATION);
				builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(user.account_id));
				builder.appendQueryParameter(QUERY_PARAM_RECIPIENT_ID, String.valueOf(user.id));
				startActivity(new Intent(Intent.ACTION_VIEW, builder.build()));
				break;
			}
			case MENU_SET_COLOR: {
				final Intent intent = new Intent(getActivity(), ColorPickerDialogActivity.class);
				intent.putExtra(EXTRA_COLOR, getUserColor(getActivity(), user.id, true));
				intent.putExtra(EXTRA_ALPHA_SLIDER, false);
				intent.putExtra(EXTRA_CLEAR_BUTTON, true);
				startActivityForResult(intent, REQUEST_SET_COLOR);
				break;
			}
			case MENU_ADD_TO_LIST: {
				final Intent intent = new Intent(INTENT_ACTION_SELECT_USER_LIST);
				intent.setClass(getActivity(), UserListSelectorActivity.class);
				intent.putExtra(EXTRA_ACCOUNT_ID, user.account_id);
				intent.putExtra(EXTRA_SCREEN_NAME, getAccountScreenName(getActivity(), user.account_id));
				startActivityForResult(intent, REQUEST_ADD_TO_LIST);
				break;
			}
			case MENU_OPEN_WITH_ACCOUNT: {
				final Intent intent = new Intent(INTENT_ACTION_SELECT_ACCOUNT);
				intent.setClass(getActivity(), AccountSelectorActivity.class);
				intent.putExtra(EXTRA_SINGLE_SELECTION, true);
				startActivityForResult(intent, REQUEST_SELECT_ACCOUNT);
				break;
			}
			case MENU_FOLLOW: {
				if (relationship == null) return false;
				final boolean isFollowing = relationship.isSourceFollowingTarget();
				final boolean isCreatingFriendship = twitter.isCreatingFriendship(user.account_id, user.id);
				final boolean isDestroyingFriendship = twitter.isDestroyingFriendship(user.account_id, user.id);
				if (!isCreatingFriendship && !isDestroyingFriendship) {
					if (isFollowing) {
						DestroyFriendshipDialogFragment.show(getFragmentManager(), user);
					} else {
						twitter.createFriendshipAsync(user.account_id, user.id);
					}
				}
				return true;
			}
			default: {
				if (item.getIntent() != null) {
					try {
						startActivity(item.getIntent());
					} catch (final ActivityNotFoundException e) {
						if (Utils.isDebugBuild()) Log.w(LOGTAG, e);
						return false;
					}
				}
				break;
			}
		}
		return true;
	}

	private void updateFollowProgressState() {
		final AsyncTwitterWrapper twitter = getTwitterWrapper();
		final ParcelableUser user = mUser;
		if (twitter == null || user == null) {
			mFollowButton.setVisibility(View.GONE);
			mFollowProgress.setVisibility(View.GONE);
			return;
		}
		final LoaderManager lm = getLoaderManager();
		final boolean loadingRelationship = lm.getLoader(LOADER_ID_FRIENDSHIP) != null;
		final boolean creatingFriendship = twitter.isCreatingFriendship(user.account_id, user.id);
		final boolean destroyingFriendship = twitter.isDestroyingFriendship(user.account_id, user.id);
		final boolean creatingBlock = twitter.isCreatingFriendship(user.account_id, user.id);
		final boolean destroyingBlock = twitter.isDestroyingFriendship(user.account_id, user.id);
		if (loadingRelationship || creatingFriendship || destroyingFriendship || creatingBlock || destroyingBlock) {
			mFollowButton.setVisibility(View.GONE);
			mFollowProgress.setVisibility(View.VISIBLE);
		} else if (mRelationship != null) {
			mFollowButton.setVisibility(View.VISIBLE);
			mFollowProgress.setVisibility(View.GONE);
		} else {
			mFollowButton.setVisibility(View.GONE);
			mFollowProgress.setVisibility(View.GONE);
		}
	}

	private void setMenu(final Menu menu) {
		final AsyncTwitterWrapper twitter = getTwitterWrapper();
		final ParcelableUser user = mUser;
		final Relationship relationship = mRelationship;
		if (twitter == null || user == null) return;
		final boolean isMyself = user.account_id == user.id;
		final MenuItem mentionItem = menu.findItem(MENU_MENTION);
		if (mentionItem != null) {
            mentionItem.setTitle(getString(R.string.mention_user_name, getDisplayName(user.name, user.screen_name)));
		}
        Utils.setMenuItemAvailability(menu, MENU_MENTION, !isMyself);
//        final MenuItem followItem = menu.findItem(MENU_FOLLOW);
//        followItem.setVisible(!isMyself);
//        final boolean shouldShowFollowItem = !creatingFriendship && !destroyingFriendship && !isMyself
//                && relationship != null;
//        followItem.setEnabled(shouldShowFollowItem);
//        if (shouldShowFollowItem) {
//            followItem.setTitle(isFollowing ? R.string.unfollow : isProtected ? R.string.send_follow_request
//                    : R.string.follow);
//            followItem.setIcon(isFollowing ? R.drawable.ic_action_cancel : R.drawable.ic_action_add);
//        } else {
//            followItem.setTitle(null);
//            followItem.setIcon(null);
//        }
        if (!isMyself && relationship != null) {
			setMenuItemAvailability(menu, MENU_SEND_DIRECT_MESSAGE, relationship.canSourceDMTarget());
			setMenuItemAvailability(menu, MENU_BLOCK, true);
			setMenuItemAvailability(menu, MENU_MUTE_USER, true);
			final MenuItem blockItem = menu.findItem(MENU_BLOCK);
			if (blockItem != null) {
				final boolean blocking = relationship.isSourceBlockingTarget();
				MenuUtils.setMenuInfo(blockItem, new TwidereMenuInfo(blocking));
				blockItem.setTitle(blocking ? R.string.unblock : R.string.block);
			}
			final MenuItem muteItem = menu.findItem(MENU_MUTE_USER);
			if (muteItem != null) {
				final boolean muting = relationship.isSourceMutingTarget();
				MenuUtils.setMenuInfo(muteItem, new TwidereMenuInfo(muting));
				muteItem.setTitle(muting ? R.string.unmute : R.string.mute);
			}
			final MenuItem filterItem = menu.findItem(MENU_ADD_TO_FILTER);
			if (filterItem != null) {
				final boolean filtering = Utils.isFilteringUser(getActivity(), user.id);
				MenuUtils.setMenuInfo(filterItem, new TwidereMenuInfo(filtering));
				filterItem.setTitle(filtering ? R.string.remove_from_filter : R.string.add_to_filter);
			}
		} else {
			setMenuItemAvailability(menu, MENU_SEND_DIRECT_MESSAGE, false);
			setMenuItemAvailability(menu, MENU_BLOCK, false);
            setMenuItemAvailability(menu, MENU_MUTE_USER, false);
			setMenuItemAvailability(menu, MENU_REPORT_SPAM, false);
		}
	}

    private void setupBaseActionBar() {
        final FragmentActivity activity = getActivity();
        if (!(activity instanceof LinkHandlerActivity)) return;
        final LinkHandlerActivity linkHandler = (LinkHandlerActivity) activity;
        final ActionBar actionBar = linkHandler.getActionBar();
        if (actionBar == null) return;
        final int themeResId = linkHandler.getCurrentThemeResourceId();
        final Drawable shadow = activity.getResources().getDrawable(R.drawable.shadow_user_banner_action_bar);
        final Drawable background = ThemeUtils.getActionBarBackground(activity, themeResId);
        mActionBarBackground = new ActionBarDrawable(getResources(), shadow, background, ThemeUtils.isDarkTheme(themeResId));
        mActionBarBackground.setAlpha(linkHandler.getCurrentThemeBackgroundAlpha());
        mProfileBannerView.setAlpha(linkHandler.getCurrentThemeBackgroundAlpha() / 255f);
        actionBar.setBackgroundDrawable(mActionBarBackground);
	}

    private void setupUserColorActionBar(int color) {
        if (mActionBarBackground == null) {
            setupBaseActionBar();
	    }
        mActionBarBackground.setColor(color);
        mTintedStatusContent.setColor(color, ThemeUtils.getThemeAlpha(getActivity()));
        mPagerIndicator.setStripColor(color);
	}

    private void setupUserPages() {
        final Context context = getActivity();
        final Bundle args = getArguments(), tabArgs = new Bundle();
        final long accountId;
        if (args.containsKey(EXTRA_USER)) {
            final ParcelableUser user = args.getParcelable(EXTRA_USER);
            tabArgs.putLong(EXTRA_ACCOUNT_ID, accountId = user.account_id);
            tabArgs.putLong(EXTRA_USER_ID, user.id);
            tabArgs.putString(EXTRA_SCREEN_NAME, user.screen_name);
        } else {
            accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
            tabArgs.putLong(EXTRA_ACCOUNT_ID, accountId);
            tabArgs.putLong(EXTRA_USER_ID, args.getLong(EXTRA_USER_ID, -1));
            tabArgs.putString(EXTRA_SCREEN_NAME, args.getString(EXTRA_SCREEN_NAME));
			}
        mPagerAdapter.addTab(UserTimelineFragment.class, tabArgs, getString(R.string.statuses), R.drawable.ic_action_quote, 0);
        if (Utils.isOfficialKeyAccount(context, accountId)) {
            mPagerAdapter.addTab(UserMediaTimelineFragment.class, tabArgs, getString(R.string.medias), R.drawable.ic_action_gallery, 1);
	    }
        mPagerAdapter.addTab(UserFavoritesFragment.class, tabArgs, getString(R.string.favorites), R.drawable.ic_action_star, 2);
	}

    private boolean shouldUseNativeMenu() {
        return getActivity() instanceof LinkHandlerActivity;
	}

    private void updateScrollOffset(int offset) {
		final View space = mProfileBannerSpace;
        final ProfileBannerImageView profileBannerView = mProfileBannerView;
        final View profileBannerContainer = mProfileBannerContainer;
        final int spaceHeight = space.getHeight();
        final float factor = MathUtils.clamp(offset / (float) spaceHeight, 0, 1);
        profileBannerContainer.setTranslationY(Math.max(-offset, -spaceHeight));
        profileBannerView.setTranslationY(Math.min(offset, spaceHeight) / 2);

        if (mActionBarBackground != null && mTintedStatusContent != null) {
			mActionBarBackground.setFactor(factor);
            mTintedStatusContent.setFactor(factor);
		}
        updateTitleColor();
    }

    private void updateTitleColor() {
        final int[] location = new int[2];
        mNameView.getLocationOnScreen(location);
        final float nameShowingRatio = (mHeaderDrawerLayout.getPaddingTop() - location[1])
                / (float) mNameView.getHeight();
        final int textAlpha = Math.round(0xFF * MathUtils.clamp(nameShowingRatio, 0, 1));
        final FragmentActivity activity = getActivity();
        final SpannableStringBuilder spannedTitle;
        final CharSequence title = activity.getTitle();
        if (title instanceof SpannableStringBuilder) {
            spannedTitle = (SpannableStringBuilder) title;
        } else {
            spannedTitle = SpannableStringBuilder.valueOf(title);
        }
        final TextAlphaSpan[] spans = spannedTitle.getSpans(0, spannedTitle.length(), TextAlphaSpan.class);
        if (spans.length > 0) {
            spans[0].setAlpha(textAlpha);
        } else {
            spannedTitle.setSpan(new TextAlphaSpan(textAlpha), 0, spannedTitle.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        activity.setTitle(spannedTitle);
	}

    static class RelationshipLoader extends AsyncTaskLoader<SingleResponse<Relationship>> {

		private final Context context;
		private final long account_id, user_id;

        public RelationshipLoader(final Context context, final long account_id, final long user_id) {
			super(context);
			this.context = context;
			this.account_id = account_id;
			this.user_id = user_id;
		}

		@Override
		public SingleResponse<Relationship> loadInBackground() {
			if (account_id == user_id) return new SingleResponse<>(null, null);
			final Twitter twitter = getTwitterInstance(context, account_id, false);
			if (twitter == null) return new SingleResponse<>(null, null);
			try {
				final Relationship result = twitter.showFriendship(account_id, user_id);
				return new SingleResponse<>(result, null);
			} catch (final TwitterException e) {
				return new SingleResponse<>(null, e);
			}
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}
	}

	private static class ActionBarDrawable extends LayerDrawable {

		private final Drawable mShadowDrawable;
		private final Drawable mBackgroundDrawable;
		private final LineBackgroundDrawable mLineDrawable;
		private final ColorDrawable mColorDrawable;
		private final boolean mColorLineOnly;

		private float mFactor;
		private int mColor;
        private int mAlpha;

		public ActionBarDrawable(Resources resources, Drawable shadow, Drawable background,
								 boolean colorLineOnly) {
			super(new Drawable[]{shadow, background, new LineBackgroundDrawable(resources, 2.0f),
                    new ActionBarColorDrawable()});
			mShadowDrawable = shadow;
			mBackgroundDrawable = getDrawable(1);
			mLineDrawable = (LineBackgroundDrawable) getDrawable(2);
			mColorDrawable = (ColorDrawable) getDrawable(3);
			mColorLineOnly = colorLineOnly;
            setAlpha(0xFF);
		}

		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		@Override
		public void getOutline(Outline outline) {
			final boolean showColor = !mColorLineOnly && mColor != 0;
			if (showColor) {
				mColorDrawable.getOutline(outline);
			} else {
				mBackgroundDrawable.getOutline(outline);
			}
            outline.setAlpha(mFactor * 0.99f);
		}

		@Override
        public void setAlpha(int alpha) {
            mAlpha = alpha;
            setFactor(mFactor);
        }

        @Override
		public int getIntrinsicWidth() {
			final boolean showColor = !mColorLineOnly && mColor != 0;
			if (showColor) {
				return mColorDrawable.getIntrinsicWidth();
			} else {
				return mBackgroundDrawable.getIntrinsicWidth();
			}
		}

		@Override
		public int getIntrinsicHeight() {
			final boolean showColor = !mColorLineOnly && mColor != 0;
			if (showColor) {
				return mColorDrawable.getIntrinsicHeight();
			} else {
				return mBackgroundDrawable.getIntrinsicHeight();
			}
		}

        public void setColor(int color) {
            mColor = color;
            final float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] = Math.min(hsv[2], 0.8f);
            final int processedColor = Color.HSVToColor(hsv);
            mColorDrawable.setColor(processedColor);
            mLineDrawable.setColor(color);
            setFactor(mFactor);
        }

		public void setFactor(float f) {
			mFactor = f;
            mShadowDrawable.setAlpha(Math.round(mAlpha * MathUtils.clamp(1 - f, 0, 1)));
			final boolean hasColor = mColor != 0;
			final boolean showBackground = mColorLineOnly || !hasColor;
			final boolean showLine = mColorLineOnly && hasColor;
			final boolean showColor = !mColorLineOnly && hasColor;
            mBackgroundDrawable.setAlpha(showBackground ? Math.round(mAlpha * MathUtils.clamp(f, 0, 1)) : 0);
            mLineDrawable.setAlpha(showLine ? Math.round(mAlpha * MathUtils.clamp(f, 0, 1)) : 0);
            mColorDrawable.setAlpha(showColor ? Math.round(mAlpha * MathUtils.clamp(f, 0, 1)) : 0);
		}

		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		private static class LineBackgroundDrawable extends Drawable {

			private final Rect mBounds;
			private final Paint mPaint;
			private final float mLineSize;

			private int mAlpha;
			private int mColor;

			LineBackgroundDrawable(Resources resources, float lineSizeDp) {
				mBounds = new Rect();
				mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				mLineSize = resources.getDisplayMetrics().density * lineSizeDp;
				setColor(Color.TRANSPARENT);
			}

			@Override
            public void draw(Canvas canvas) {
                canvas.drawRect(mBounds.left, mBounds.bottom - mLineSize, mBounds.right,
                        mBounds.bottom, mPaint);
            }

            public int getColor() {
                return mColor;
            }

            public void setColor(int color) {
                mColor = color;
                updatePaint();
            }

            @Override
			protected void onBoundsChange(Rect bounds) {
				super.onBoundsChange(bounds);
				mBounds.set(bounds);
			}

            private void updatePaint() {
                mPaint.setColor(mColor);
                mPaint.setAlpha(Color.alpha(mColor) * mAlpha / 0xFF);
                invalidateSelf();
			}


			@Override
			public int getAlpha() {
				return mAlpha;
			}


			@Override
			public void setAlpha(int alpha) {
				mAlpha = alpha;
				updatePaint();
			}

			@Override
			public void setColorFilter(ColorFilter cf) {

			}

			@Override
			public int getOpacity() {
				return PixelFormat.TRANSLUCENT;
			}

		}
	}
}