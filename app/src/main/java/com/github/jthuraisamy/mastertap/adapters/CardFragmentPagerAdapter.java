package com.github.jthuraisamy.mastertap.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;
import com.github.jthuraisamy.mastertap.fragments.AddCardFragment;
import com.github.jthuraisamy.mastertap.fragments.CardFragment;

public class CardFragmentPagerAdapter extends FragmentStatePagerAdapter {
	private final FragmentActivity ctx;

	public CardFragmentPagerAdapter(FragmentActivity ctx) {
		super(ctx.getSupportFragmentManager());

		this.ctx = ctx;
	}

	@Override
	public int getCount() {
		return MainActivity.cards.size();
	}

	@Override
	public Fragment getItem(int position) {
		switch (position) {
			case 0:
				return AddCardFragment.create();
			default:
				return CardFragment.create(position);
		}
	}

	@Override
	public int getItemPosition(@NonNull Object object) {
		return POSITION_NONE;
	}

	public CharSequence getPageTitle(int position) {
		switch (position) {
			case 0:
				return ctx.getText(R.string.add_card);
			default:
				return MainActivity.cards.get(position).getLabel();
		}
	}
}
