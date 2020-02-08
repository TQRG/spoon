package me.ccrama.redditslide.Adapters;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.Html;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.cocosw.bottomsheet.BottomSheet;

import net.dean.jraw.ApiException;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.oauth.InvalidScopeException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.managers.ModerationManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.DistinguishedStatus;
import net.dean.jraw.models.Ruleset;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditRule;
import net.dean.jraw.models.VoteDirection;

import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.ccrama.redditslide.ActionStates;
import me.ccrama.redditslide.Activities.Profile;
import me.ccrama.redditslide.Activities.Reauthenticate;
import me.ccrama.redditslide.Activities.Website;
import me.ccrama.redditslide.Authentication;
import me.ccrama.redditslide.OpenRedditLink;
import me.ccrama.redditslide.R;
import me.ccrama.redditslide.Reddit;
import me.ccrama.redditslide.SettingValues;
import me.ccrama.redditslide.SpoilerRobotoTextView;
import me.ccrama.redditslide.TimeUtils;
import me.ccrama.redditslide.Toolbox.Toolbox;
import me.ccrama.redditslide.Toolbox.ToolboxUI;
import me.ccrama.redditslide.UserSubscriptions;
import me.ccrama.redditslide.UserTags;
import me.ccrama.redditslide.Views.CommentOverflow;
import me.ccrama.redditslide.Views.DoEditorActions;
import me.ccrama.redditslide.Views.RoundedBackgroundSpan;
import me.ccrama.redditslide.Visuals.FontPreferences;
import me.ccrama.redditslide.Visuals.Palette;
import me.ccrama.redditslide.util.LinkUtil;

/**
 * Created by Carlos on 8/4/2016.
 */
public class CommentAdapterHelper {

    public static void stickyComment(final Context mContext, final CommentViewHolder holder,
                                     final Comment comment) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            public void onPostExecute(Boolean b) {
                if (b) {
                    Snackbar s = Snackbar.make(holder.itemView, R.string.comment_stickied,
                            Snackbar.LENGTH_LONG);
                    View view = s.getView();
                    TextView tv = view.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    s.show();
                } else {
                    new AlertDialogWrapper.Builder(mContext).setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show();
                }
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    new ModerationManager(Authentication.reddit).setSticky(comment, true);
                } catch (ApiException e) {
                    e.printStackTrace();
                    return false;

                }
                return true;
            }
        }.execute();
    }

    public static void viewReports(final Context mContext, final Map<String, Integer> reports,
                                   final Map<String, String> reports2) {
        new AsyncTask<Void, Void, ArrayList<String>>() {
            @Override
            protected ArrayList<String> doInBackground(Void... params) {

                ArrayList<String> finalReports = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : reports.entrySet()) {
                    finalReports.add("x" + entry.getValue() + " " + entry.getKey());
                }
                for (Map.Entry<String, String> entry : reports2.entrySet()) {
                    finalReports.add(entry.getKey() + ": " + entry.getValue());
                }
                if (finalReports.isEmpty()) {
                    finalReports.add(mContext.getString(R.string.mod_no_reports));
                }
                return finalReports;
            }

            @Override
            public void onPostExecute(ArrayList<String> data) {
                new AlertDialogWrapper.Builder(mContext).setTitle(R.string.mod_reports)
                        .setItems(data.toArray(new CharSequence[data.size()]),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                        .show();
            }
        }.execute();
    }

    public static void doApproval(final Context mContext, final CommentViewHolder holder,
                                  final Comment comment, final CommentAdapter adapter) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            public void onPostExecute(Boolean b) {
                if (b) {
                    adapter.approved.add(comment.getFullName());
                    adapter.removed.remove(comment.getFullName());
                    holder.content.setText(
                            CommentAdapterHelper.getScoreString(comment, mContext, holder,
                                    adapter.submission, adapter));
                    Snackbar.make(holder.itemView, R.string.mod_approved, Snackbar.LENGTH_LONG)
                            .show();

                } else {
                    new AlertDialogWrapper.Builder(mContext).setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show();
                }
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    new ModerationManager(Authentication.reddit).approve(comment);
                } catch (ApiException e) {
                    e.printStackTrace();
                    return false;

                }
                return true;
            }
        }.execute();
    }

    public static void unStickyComment(final Context mContext, final CommentViewHolder holder,
                                       final Comment comment) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            public void onPostExecute(Boolean b) {
                if (b) {
                    Snackbar s = Snackbar.make(holder.itemView, R.string.comment_unstickied,
                            Snackbar.LENGTH_LONG);
                    View view = s.getView();
                    TextView tv = view.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    s.show();
                } else {
                    new AlertDialogWrapper.Builder(mContext).setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show();
                }
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    new ModerationManager(Authentication.reddit).setSticky(comment, false);
                } catch (ApiException e) {
                    e.printStackTrace();
                    return false;

                }
                return true;
            }
        }.execute();
    }

    public static void removeComment(final Context mContext, final CommentViewHolder holder,
                                     final Comment comment, final CommentAdapter adapter, final boolean spam) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            public void onPostExecute(Boolean b) {
                if (b) {
                    Snackbar s = Snackbar.make(holder.itemView, R.string.comment_removed,
                            Snackbar.LENGTH_LONG);
                    View view = s.getView();
                    TextView tv = view.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    s.show();

                    adapter.removed.add(comment.getFullName());
                    adapter.approved.remove(comment.getFullName());
                    holder.content.setText(
                            CommentAdapterHelper.getScoreString(comment, mContext, holder,
                                    adapter.submission, adapter));
                } else {
                    new AlertDialogWrapper.Builder(mContext).setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show();
                }
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    new ModerationManager(Authentication.reddit).remove(comment, spam);
                } catch (ApiException | NetworkException e) {
                    e.printStackTrace();
                    return false;

                }
                return true;
            }
        }.execute();
    }

    /**
     * Show a removal dialog to input a reason, then remove comment and post reason
     * @param mContext context
     * @param holder commentviewholder
     * @param comment comment
     * @param adapter commentadapter
     */
    public static void doRemoveCommentReason(final Context mContext,
                                             final CommentViewHolder holder, final Comment comment, final CommentAdapter adapter) {
        new MaterialDialog.Builder(mContext).title(R.string.mod_remove_title)
                .positiveText(R.string.btn_remove)
                .alwaysCallInputCallback()
                .input(mContext.getString(R.string.mod_remove_hint),
                        mContext.getString(R.string.mod_remove_template), false,
                        new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                            }
                        })
                .inputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                .neutralText(R.string.mod_remove_insert_draft)
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog,
                                        @NonNull DialogAction which) {

                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, DialogAction which) {
                        removeCommentReason(comment, mContext, holder, adapter,
                                dialog.getInputEditText().getText().toString());
                    }
                })
                .negativeText(R.string.btn_cancel)
                .show();
    }

    /**
     * Remove a comment and post a reason
     * @param comment comment
     * @param mContext context
     * @param holder commentviewholder
     * @param adapter commentadapter
     * @param reason reason
     */
    public static void removeCommentReason(final Comment comment, final Context mContext, CommentViewHolder holder,
                                           final CommentAdapter adapter, final String reason) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            public void onPostExecute(Boolean b) {
                if (b) {
                    Snackbar s = Snackbar.make(holder.itemView, R.string.comment_removed, Snackbar.LENGTH_LONG);
                    View view = s.getView();
                    TextView tv = view.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    s.show();

                    adapter.removed.add(comment.getFullName());
                    adapter.approved.remove(comment.getFullName());
                    holder.content.setText(CommentAdapterHelper.getScoreString(comment, mContext, holder,
                            adapter.submission, adapter));
                } else {
                    new AlertDialogWrapper.Builder(mContext).setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show();
                }
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    new AccountManager(Authentication.reddit).reply(comment, reason);
                    new ModerationManager(Authentication.reddit).remove(comment, false);
                    new ModerationManager(Authentication.reddit).setDistinguishedStatus(
                            Authentication.reddit.get(comment.getFullName()).get(0),
                            DistinguishedStatus.MODERATOR);
                } catch (ApiException | NetworkException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static SpannableStringBuilder createApprovedLine(String approvedBy, Context c) {
        SpannableStringBuilder removedString = new SpannableStringBuilder("\n");
        SpannableStringBuilder mod = new SpannableStringBuilder("Approved by ");
        mod.append(approvedBy);
        mod.setSpan(new StyleSpan(Typeface.BOLD), 0, mod.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mod.setSpan(new RelativeSizeSpan(0.8f), 0, mod.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mod.setSpan(new ForegroundColorSpan(c.getResources().getColor(R.color.md_green_300)), 0,
                mod.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        removedString.append(mod);
        return removedString;
    }

    public static SpannableStringBuilder createRemovedLine(String removedBy, Context c) {
        SpannableStringBuilder removedString = new SpannableStringBuilder("\n");
        SpannableStringBuilder mod = new SpannableStringBuilder("Removed by ");
        if (removedBy.equalsIgnoreCase(
                "true")) {//Probably shadowbanned or removed not by mod action
            mod = new SpannableStringBuilder("Removed by Reddit");
        } else {
            mod.append(removedBy);
        }
        mod.setSpan(new StyleSpan(Typeface.BOLD), 0, mod.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mod.setSpan(new RelativeSizeSpan(0.8f), 0, mod.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mod.setSpan(new ForegroundColorSpan(c.getResources().getColor(R.color.md_red_300)), 0,
                mod.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        removedString.append(mod);
        return removedString;
    }

    public static Spannable getScoreString(Comment comment, Context mContext,
                                           CommentViewHolder holder, Submission submission, CommentAdapter adapter) {
        final String spacer =
                " " + mContext.getString(R.string.submission_properties_seperator_comments) + " ";
        SpannableStringBuilder titleString =
                new SpannableStringBuilder("\u200B");//zero width space to fix first span height
        SpannableStringBuilder author = new SpannableStringBuilder(comment.getAuthor());
        final int authorcolor = Palette.getFontColorUser(comment.getAuthor());

        author.setSpan(new TypefaceSpan("sans-serif-condensed"), 0, author.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        author.setSpan(new StyleSpan(Typeface.BOLD), 0, author.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (comment.getDistinguishedStatus() == DistinguishedStatus.ADMIN) {
            author.replace(0, author.length(), " " + comment.getAuthor() + " ");
            author.setSpan(
                    new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_red_300, false),
                    0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (comment.getDistinguishedStatus() == DistinguishedStatus.SPECIAL) {
            author.replace(0, author.length(), " " + comment.getAuthor() + " ");
            author.setSpan(
                    new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_red_500, false),
                    0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (comment.getDistinguishedStatus() == DistinguishedStatus.MODERATOR) {
            author.replace(0, author.length(), " " + comment.getAuthor() + " ");
            author.setSpan(
                    new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_green_300, false),
                    0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (Authentication.name != null && comment.getAuthor()
                .toLowerCase(Locale.ENGLISH)
                .equals(Authentication.name.toLowerCase(Locale.ENGLISH))) {
            author.replace(0, author.length(), " " + comment.getAuthor() + " ");
            author.setSpan(
                    new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_deep_orange_300,
                            false), 0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (submission != null && comment.getAuthor()
                .toLowerCase(Locale.ENGLISH)
                .equals(submission.getAuthor().toLowerCase(Locale.ENGLISH)) && !comment.getAuthor()
                .equals("[deleted]")) {
            author.replace(0, author.length(), " " + comment.getAuthor() + " ");
            author.setSpan(
                    new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_blue_300, false),
                    0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (authorcolor != 0) {
            author.setSpan(new ForegroundColorSpan(authorcolor), 0, author.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        titleString.append(author);
        titleString.append(spacer);

        int scoreColor;
        switch (ActionStates.getVoteDirection(comment)) {
            case UPVOTE:
                scoreColor = (holder.textColorUp);
                break;
            case DOWNVOTE:
                scoreColor = (holder.textColorDown);
                break;
            default:
                scoreColor = (holder.textColorRegular);
                break;
        }

        String scoreText;
        if (comment.isScoreHidden()) {
            scoreText = "[" + mContext.getString(R.string.misc_score_hidden).toUpperCase() + "]";
        } else {
            scoreText = String.format(Locale.getDefault(), "%d", getScoreText(comment));
        }

        SpannableStringBuilder score = new SpannableStringBuilder(scoreText);

        if (score == null || score.toString().isEmpty()) {
            score = new SpannableStringBuilder("0");
        }
        if (!scoreText.contains("[")) {
            score.append(String.format(Locale.getDefault(), " %s", mContext.getResources()
                    .getQuantityString(R.plurals.points, comment.getScore())));
        }
        score.setSpan(new ForegroundColorSpan(scoreColor), 0, score.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        titleString.append(score);
        titleString.append((comment.isControversial() ? " †" : ""));

        titleString.append(spacer);

        Long time = comment.getCreated().getTime();
        String timeAgo = TimeUtils.getTimeAgo(time, mContext);

        SpannableStringBuilder timeSpan = new SpannableStringBuilder().append(
                (timeAgo == null || timeAgo.isEmpty()) ? "just now" : timeAgo);

        if (SettingValues.highlightTime
                && adapter.lastSeen != 0
                && adapter.lastSeen < time
                && !adapter.dataSet.single
                && SettingValues.commentLastVisit) {
            timeSpan.setSpan(new RoundedBackgroundSpan(Color.WHITE,
                            Palette.getColor(comment.getSubredditName()), false, mContext), 0,
                    timeSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        }
        titleString.append(timeSpan);

        titleString.append(((comment.getEditDate() != null) ? " (edit " + TimeUtils.getTimeAgo(
                comment.getEditDate().getTime(), mContext) + ")" : ""));
        titleString.append("  ");

        if (comment.getDataNode().get("stickied").asBoolean()) {
            SpannableStringBuilder pinned = new SpannableStringBuilder("\u00A0"
                    + mContext.getString(R.string.submission_stickied).toUpperCase()
                    + "\u00A0");
            pinned.setSpan(
                    new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_green_300, false),
                    0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleString.append(pinned);
            titleString.append(" ");
        }
        if (comment.getTimesSilvered() > 0 || comment.getTimesGilded() > 0  || comment.getTimesPlatinized() > 0) {
            TypedArray a = mContext.obtainStyledAttributes(
                    new FontPreferences(mContext).getPostFontStyle().getResId(),
                    R.styleable.FontStyle);
            int fontsize =
                    (int) (a.getDimensionPixelSize(R.styleable.FontStyle_font_cardtitle, -1) * .75);
            a.recycle();
            // Add silver, gold, platinum icons and counts in that order
            if (comment.getTimesSilvered() > 0) {
                final String timesSilvered = (comment.getTimesSilvered() == 1) ? ""
                        : "\u200Ax" + Integer.toString(comment.getTimesSilvered());
                SpannableStringBuilder silvered =
                        new SpannableStringBuilder("\u00A0★" + timesSilvered + "\u00A0");
                Bitmap image = adapter.awardIcons[0];
                float aspectRatio = (float) (1.00 * image.getWidth() / image.getHeight());
                image = Bitmap.createScaledBitmap(image, (int) Math.ceil(fontsize * aspectRatio),
                        (int) Math.ceil(fontsize), true);
                silvered.setSpan(new ImageSpan(mContext, image, ImageSpan.ALIGN_BASELINE), 0, 2,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                silvered.setSpan(new RelativeSizeSpan(0.75f), 3, silvered.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                titleString.append(silvered);
                titleString.append(" ");
            }
            if (comment.getTimesGilded() > 0) {
                final String timesGilded = (comment.getTimesGilded() == 1) ? ""
                        : "\u200Ax" + Integer.toString(comment.getTimesGilded());
                SpannableStringBuilder gilded =
                        new SpannableStringBuilder("\u00A0★" + timesGilded + "\u00A0");
                Bitmap image = adapter.awardIcons[1];
                float aspectRatio = (float) (1.00 * image.getWidth() / image.getHeight());
                image = Bitmap.createScaledBitmap(image, (int) Math.ceil(fontsize * aspectRatio),
                        (int) Math.ceil(fontsize), true);
                gilded.setSpan(new ImageSpan(mContext, image, ImageSpan.ALIGN_BASELINE), 0, 2,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                gilded.setSpan(new RelativeSizeSpan(0.75f), 3, gilded.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                titleString.append(gilded);
                titleString.append(" ");
            }
            if (comment.getTimesPlatinized() > 0) {
                final String timesPlatinized = (comment.getTimesPlatinized() == 1) ? ""
                        : "\u200Ax" + Integer.toString(comment.getTimesPlatinized());
                SpannableStringBuilder platinized =
                        new SpannableStringBuilder("\u00A0★" + timesPlatinized + "\u00A0");
                Bitmap image = adapter.awardIcons[2];
                float aspectRatio = (float) (1.00 * image.getWidth() / image.getHeight());
                image = Bitmap.createScaledBitmap(image, (int) Math.ceil(fontsize * aspectRatio),
                        (int) Math.ceil(fontsize), true);
                platinized.setSpan(new ImageSpan(mContext, image, ImageSpan.ALIGN_BASELINE), 0, 2,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                platinized.setSpan(new RelativeSizeSpan(0.75f), 3, platinized.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                titleString.append(platinized);
                titleString.append(" ");
            }
        }
        if (UserTags.isUserTagged(comment.getAuthor())) {
            SpannableStringBuilder pinned = new SpannableStringBuilder(
                    "\u00A0" + UserTags.getUserTag(comment.getAuthor()) + "\u00A0");
            pinned.setSpan(
                    new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_blue_500, false),
                    0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleString.append(pinned);
            titleString.append(" ");
        }
        if (UserSubscriptions.friends.contains(comment.getAuthor())) {
            SpannableStringBuilder pinned = new SpannableStringBuilder(
                    "\u00A0" + mContext.getString(R.string.profile_friend) + "\u00A0");
            pinned.setSpan(
                    new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_deep_orange_500,
                            false), 0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleString.append(pinned);
            titleString.append(" ");
        }
        if (comment.getAuthorFlair() != null && (comment.getAuthorFlair().getText() != null
                || comment.getAuthorFlair().getCssClass() != null)) {

            String flairText = null;
            if (comment.getAuthorFlair() != null &&
                    comment.getAuthorFlair().getText() != null &&
                    !comment.getAuthorFlair().getText().isEmpty()) {

                flairText = comment.getAuthorFlair().getText();

            } else if (comment.getAuthorFlair() != null &&
                    comment.getAuthorFlair().getCssClass() != null &&
                    !comment.getAuthorFlair().getCssClass().isEmpty()) {

                flairText = comment.getAuthorFlair().getCssClass();
            }

            if (flairText != null) {
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = mContext.getTheme();
                theme.resolveAttribute(R.attr.activity_background, typedValue, true);
                int color = typedValue.data;
                SpannableStringBuilder pinned =
                        new SpannableStringBuilder("\u00A0" + Html.fromHtml(flairText) + "\u00A0");
                pinned.setSpan(
                        new RoundedBackgroundSpan(holder.firstTextView.getCurrentTextColor(), color,
                                false, mContext), 0, pinned.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                titleString.append(pinned);
                titleString.append(" ");
            }
        }

        ToolboxUI.appendToolboxNote(mContext, titleString, comment.getSubredditName(), comment.getAuthor());

        if (adapter.removed.contains(comment.getFullName()) || (comment.getBannedBy() != null
                && !adapter.approved.contains(comment.getFullName()))) {
            titleString.append(CommentAdapterHelper.createRemovedLine(
                    (comment.getBannedBy() == null) ? Authentication.name : comment.getBannedBy(),
                    mContext));
        } else if (adapter.approved.contains(comment.getFullName()) || (comment.getApprovedBy()
                != null && !adapter.removed.contains(comment.getFullName()))) {
            titleString.append(CommentAdapterHelper.createApprovedLine(
                    (comment.getApprovedBy() == null) ? Authentication.name
                            : comment.getApprovedBy(), mContext));
        }
        return titleString;
    }

    public static int getScoreText(Comment comment) {
        int submissionScore = comment.getScore();
        switch (ActionStates.getVoteDirection(comment)) {
            case UPVOTE: {
                if (comment.getVote() != VoteDirection.UPVOTE) {
                    if (comment.getVote() == VoteDirection.DOWNVOTE) ++submissionScore;
                    ++submissionScore; //offset the score by +1
                }
                break;
            }
            case DOWNVOTE: {
                if (comment.getVote() != VoteDirection.DOWNVOTE) {
                    if (comment.getVote() == VoteDirection.UPVOTE) --submissionScore;
                    --submissionScore; //offset the score by +1
                }
                break;
            }
            case NO_VOTE:
                if (comment.getVote() == VoteDirection.UPVOTE && comment.getAuthor()
                        .equalsIgnoreCase(Authentication.name)) {
                    submissionScore--;
                }
                break;
        }
        return submissionScore;
    }

    public static void doCommentEdit(final CommentAdapter adapter, final Context mContext,
                                     FragmentManager fm, final CommentNode baseNode, String replyText,
                                     final CommentViewHolder holder) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();

        final View dialoglayout = inflater.inflate(R.layout.edit_comment, null);
        final AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(mContext);

        final EditText e = dialoglayout.findViewById(R.id.entry);
        e.setText(StringEscapeUtils.unescapeHtml4(baseNode.getComment().getBody()));

        DoEditorActions.doActions(e, dialoglayout, fm, (Activity) mContext,
                StringEscapeUtils.unescapeHtml4(replyText), null);

        builder.setCancelable(false).setView(dialoglayout);
        final Dialog d = builder.create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        d.show();
        dialoglayout.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        dialoglayout.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = e.getText().toString();
                new AsyncEditTask(adapter, baseNode, text, mContext, d, holder).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

    }

    public static void deleteComment(final CommentAdapter adapter, final Context mContext,
                                     final CommentNode baseNode, final CommentViewHolder holder) {
        new AlertDialogWrapper.Builder(mContext).setTitle(R.string.comment_delete)
                .setMessage(R.string.comment_delete_msg)
                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncDeleteTask(adapter, baseNode, holder, mContext).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                })
                .setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public static class AsyncEditTask extends AsyncTask<Void, Void, Void> {
        CommentAdapter    adapter;
        CommentNode       baseNode;
        String            text;
        Context           mContext;
        Dialog            dialog;
        CommentViewHolder holder;

        public AsyncEditTask(CommentAdapter adapter, CommentNode baseNode, String text,
                             Context mContext, Dialog dialog, CommentViewHolder holder) {
            this.adapter = adapter;
            this.baseNode = baseNode;
            this.text = text;
            this.mContext = mContext;
            this.dialog = dialog;
            this.holder = holder;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                new AccountManager(Authentication.reddit).updateContribution(baseNode.getComment(),
                        text);
                adapter.currentSelectedItem = baseNode.getComment().getFullName();
                CommentNode n = baseNode.notifyCommentChanged(Authentication.reddit);
                adapter.editComment(n, holder);
                dialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialogWrapper.Builder(mContext).setTitle(
                                R.string.comment_delete_err)
                                .setMessage(R.string.comment_delete_err_msg)
                                .setPositiveButton(R.string.btn_yes,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                new AsyncEditTask(adapter, baseNode, text, mContext,
                                                        AsyncEditTask.this.dialog,
                                                        holder).execute();
                                            }
                                        })
                                .setNegativeButton(R.string.btn_no,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                .show();
                    }
                });
            }
            return null;
        }
    }

    public static class AsyncDeleteTask extends AsyncTask<Void, Void, Boolean> {
        CommentAdapter    adapter;
        CommentNode       baseNode;
        CommentViewHolder holder;
        Context           mContext;

        public AsyncDeleteTask(CommentAdapter adapter, CommentNode baseNode,
                               CommentViewHolder holder, Context mContext) {
            this.adapter = adapter;
            this.baseNode = baseNode;
            this.holder = holder;
            this.mContext = mContext;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                holder.firstTextView.setTextHtml(mContext.getString(R.string.content_deleted));
                holder.content.setText(R.string.content_deleted);
            } else {
                new AlertDialogWrapper.Builder(mContext).setTitle(R.string.comment_delete_err)
                        .setMessage(R.string.comment_delete_err_msg)
                        .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                doInBackground();
                            }
                        })
                        .setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                new ModerationManager(Authentication.reddit).delete(baseNode.getComment());
                adapter.deleted.add(baseNode.getComment().getFullName());
                return true;
            } catch (ApiException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static class AsyncReportTask extends AsyncTask<String, Void, Void> {
        private CommentNode baseNode;
        private View        contextView;

        public AsyncReportTask(CommentNode baseNode, View contextView) {
            this.baseNode = baseNode;
            this.contextView = contextView;
        }

        @Override
        protected Void doInBackground(String... reason) {
            try {
                new AccountManager(Authentication.reddit).report(baseNode.getComment(), reason[0]);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Snackbar s =
                    Snackbar.make(contextView, R.string.msg_report_sent, Snackbar.LENGTH_SHORT);
            View view = s.getView();
            TextView tv = view.findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            s.show();
        }
    }

    public static void showChildrenObject(final View v) {
        v.setVisibility(View.VISIBLE);
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1f);
        animator.setDuration(250);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) (animation.getAnimatedValue())).floatValue();
                v.setAlpha(value);
                v.setScaleX(value);
                v.setScaleY(value);
            }
        });

        animator.start();
    }

    public static void hideChildrenObject(final View v) {
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0);
        animator.setDuration(250);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {


            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) (animation.getAnimatedValue())).floatValue();
                v.setAlpha(value);
                v.setScaleX(value);
                v.setScaleY(value);

            }
        });

        animator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {

            }

            @Override
            public void onAnimationRepeat(Animator arg0) {

            }

            @Override
            public void onAnimationEnd(Animator arg0) {

                v.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator arg0) {
                v.setVisibility(View.GONE);

            }
        });

        animator.start();
    }

}
