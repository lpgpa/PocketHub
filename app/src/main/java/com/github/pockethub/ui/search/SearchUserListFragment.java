/*
 * Copyright 2013 GitHub Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pockethub.ui.search;

import static android.app.SearchManager.QUERY;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.alorma.github.sdk.bean.dto.response.Repo;
import com.alorma.github.sdk.services.client.GithubClient;
import com.alorma.github.sdk.services.search.RepoSearchClient;
import com.alorma.github.sdk.services.search.UsersSearchClient;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.github.pockethub.R;
import com.github.pockethub.ThrowableLoader;
import com.github.pockethub.accounts.AccountUtils;
import com.github.pockethub.core.PageIterator;
import com.github.pockethub.core.ResourcePager;
import com.github.pockethub.core.search.SearchUser;
import com.github.pockethub.core.search.SearchUserService;
import com.github.pockethub.core.user.RefreshUserTask;
import com.github.pockethub.ui.ItemListFragment;
import com.github.pockethub.ui.PagedItemFragment;
import com.github.pockethub.ui.user.UserViewActivity;
import com.github.pockethub.util.AvatarLoader;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import com.alorma.github.sdk.bean.dto.response.User;

/**
 * Fragment to display a list of {@link SearchUser} instances
 */
public class SearchUserListFragment extends PagedItemFragment<User> {

    private String query;

    @Inject
    private SearchUserService service;

    @Inject
    private AvatarLoader avatars;

    @Override
    protected ResourcePager<User> createPager() {
        return new ResourcePager<User>() {
            @Override
            protected Object getId(User resource) {
                return resource.id;
            }

            @Override
            public PageIterator<User> createIterator(int page, int size) {
                return new PageIterator<>(new PageIterator.GitHubRequest<List<User>>() {
                    @Override
                    public GithubClient<List<User>> execute(int page) {
                        return new UsersSearchClient(getContext(), query, page);
                    }
                }, page);
            }
        };
    }

    @Override
    protected int getLoadingMessage() {
        return R.string.loading_user;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(R.string.no_people);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        query = getStringExtra(QUERY);
    }

    @Override
    public void refresh() {
        query = getStringExtra(QUERY);

        super.refresh();
    }

    @Override
    protected SingleTypeAdapter<User> createAdapter(List<User> items) {
        return new SearchUserListAdapter(getActivity(),
                items.toArray(new User[items.size()]), avatars);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final User result = (User) l.getItemAtPosition(position);
        new RefreshUserTask(getActivity(), result.login) {

            @Override
            protected void onSuccess(User user) throws Exception {
                super.onSuccess(user);

                if (!AccountUtils.isUser(getActivity(), user))
                    startActivity(UserViewActivity.createIntent(user));
            }
        }.execute();
    }

    @Override
    protected int getErrorMessage(Exception exception) {
        return R.string.error_users_search;
    }
}
