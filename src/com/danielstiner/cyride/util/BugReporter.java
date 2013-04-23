package com.danielstiner.cyride.util;

import java.util.Properties;

import com.danielstiner.cyride.R;

import de.akquinet.android.androlog.reporter.MailReporter;

import android.content.Context;
import android.widget.Toast;

public class BugReporter implements
		de.akquinet.android.androlog.reporter.Reporter {

	private MailReporter reporter = new MailReporter();

	@Override
	public void configure(Properties configuration) {
		reporter.configure(configuration);
	}

	@Override
	public boolean send(Context context, String message, Throwable error) {

		Toast.makeText(context, R.string.crash_toast_text, Toast.LENGTH_LONG)
				.show();

		return reporter.send(context, message, error);
	}

}