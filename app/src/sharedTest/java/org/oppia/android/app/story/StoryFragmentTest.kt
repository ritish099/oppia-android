package org.oppia.android.app.story

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import dagger.Component
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.oppia.android.R
import org.oppia.android.app.activity.ActivityComponent
import org.oppia.android.app.application.ActivityComponentFactory
import org.oppia.android.app.application.ApplicationComponent
import org.oppia.android.app.application.ApplicationInjector
import org.oppia.android.app.application.ApplicationInjectorProvider
import org.oppia.android.app.application.ApplicationModule
import org.oppia.android.app.application.ApplicationStartupListenerModule
import org.oppia.android.app.model.ProfileId
import org.oppia.android.app.player.exploration.ExplorationActivity
import org.oppia.android.app.player.state.hintsandsolution.HintsAndSolutionConfigModule
import org.oppia.android.app.recyclerview.RecyclerViewMatcher.Companion.atPositionOnView
import org.oppia.android.app.recyclerview.RecyclerViewMatcher.Companion.hasItemCount
import org.oppia.android.app.shim.ViewBindingShimModule
import org.oppia.android.app.utility.OrientationChangeAction.Companion.orientationLandscape
import org.oppia.android.domain.classify.InteractionsModule
import org.oppia.android.domain.classify.rules.continueinteraction.ContinueModule
import org.oppia.android.domain.classify.rules.dragAndDropSortInput.DragDropSortInputModule
import org.oppia.android.domain.classify.rules.fractioninput.FractionInputModule
import org.oppia.android.domain.classify.rules.imageClickInput.ImageClickInputModule
import org.oppia.android.domain.classify.rules.itemselectioninput.ItemSelectionInputModule
import org.oppia.android.domain.classify.rules.multiplechoiceinput.MultipleChoiceInputModule
import org.oppia.android.domain.classify.rules.numberwithunits.NumberWithUnitsRuleModule
import org.oppia.android.domain.classify.rules.numericinput.NumericInputRuleModule
import org.oppia.android.domain.classify.rules.ratioinput.RatioInputModule
import org.oppia.android.domain.classify.rules.textinput.TextInputRuleModule
import org.oppia.android.domain.onboarding.ExpirationMetaDataRetrieverModule
import org.oppia.android.domain.oppialogger.LogStorageModule
import org.oppia.android.domain.oppialogger.loguploader.LogUploadWorkerModule
import org.oppia.android.domain.oppialogger.loguploader.WorkManagerConfigurationModule
import org.oppia.android.domain.question.QuestionModule
import org.oppia.android.domain.topic.FRACTIONS_STORY_ID_0
import org.oppia.android.domain.topic.FRACTIONS_TOPIC_ID
import org.oppia.android.domain.topic.PrimeTopicAssetsControllerModule
import org.oppia.android.domain.topic.StoryProgressTestHelper
import org.oppia.android.testing.TestAccessibilityModule
import org.oppia.android.testing.TestCoroutineDispatchers
import org.oppia.android.testing.TestDispatcherModule
import org.oppia.android.testing.TestLogReportingModule
import org.oppia.android.testing.profile.ProfileTestHelper
import org.oppia.android.util.caching.testing.CachingTestModule
import org.oppia.android.util.gcsresource.GcsResourceModule
import org.oppia.android.util.logging.LoggerModule
import org.oppia.android.util.logging.firebase.FirebaseLogUploaderModule
import org.oppia.android.util.parser.GlideImageLoaderModule
import org.oppia.android.util.parser.HtmlParserEntityTypeModule
import org.oppia.android.util.parser.ImageParsingModule
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import javax.inject.Inject
import javax.inject.Singleton

/** Tests for [StoryFragment]. */
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(application = StoryFragmentTest.TestApplication::class, qualifiers = "port-xxhdpi")
class StoryFragmentTest {

  @Inject
  lateinit var profileTestHelper: ProfileTestHelper

  @Inject
  lateinit var storyProgressTestHelper: StoryProgressTestHelper

  @Inject
  lateinit var context: Context

  @Inject
  lateinit var testCoroutineDispatchers: TestCoroutineDispatchers

  @get:Rule
  var activityTestRule: ActivityTestRule<StoryActivity> = ActivityTestRule(
    StoryActivity::class.java, /* initialTouchMode= */ true, /* launchActivity= */ false
  )

  private val internalProfileId = 0

  private lateinit var profileId: ProfileId

  @Before
  fun setUp() {
    Intents.init()
    setUpTestApplicationComponent()
    testCoroutineDispatchers.registerIdlingResource()
    profileTestHelper.initializeProfiles()
    FirebaseApp.initializeApp(context)
    profileId = ProfileId.newBuilder().setInternalId(internalProfileId).build()
    storyProgressTestHelper.markPartialStoryProgressForFractions(
      profileId,
      timestampOlderThanAWeek = false
    )
    testCoroutineDispatchers.runCurrent()
  }

  @After
  fun tearDown() {
    testCoroutineDispatchers.unregisterIdlingResource()
    Intents.release()
  }

  private fun createStoryActivityIntent(): Intent {
    return StoryActivity.createStoryActivityIntent(
      ApplicationProvider.getApplicationContext(),
      internalProfileId,
      FRACTIONS_TOPIC_ID,
      FRACTIONS_STORY_ID_0
    )
  }

  @Test
  fun testStoryFragment_clickOnToolbarNavigationButton_closeActivity() {
    activityTestRule.launchActivity(createStoryActivityIntent())
    testCoroutineDispatchers.runCurrent()
    onView(withContentDescription(R.string.go_to_previous_page)).perform(click())
    assertThat(activityTestRule.activity.isFinishing).isTrue()
  }

  @Test
  fun testStoryFragment_toolbarTitle_isDisplayedSuccessfully() {
    launch<StoryActivity>(createStoryActivityIntent()).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.story_toolbar_title))
        .check(matches(withText("Matthew Goes to the Bakery")))
    }
  }

  @Test
  fun testStoryFragment_correctStoryCountLoadedInHeader() {
    launch<StoryActivity>(createStoryActivityIntent()).use {
      testCoroutineDispatchers.runCurrent()
      val headerString: String =
        getResources().getQuantityString(R.plurals.story_total_chapters, 2, 1, 2)
      onView(withId(R.id.story_chapter_list)).perform(
        scrollToPosition<RecyclerView.ViewHolder>(
          0
        )
      )
      onView(
        atPositionOnView(
          R.id.story_chapter_list,
          0,
          R.id.story_progress_chapter_completed_text
        )
      ).check(
        matches(
          withText(headerString)
        )
      )
    }
  }

  @Test
  fun testStoryFragment_correctNumberOfStoriesLoadedInRecyclerView() {
    launch<StoryActivity>(createStoryActivityIntent()).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.story_chapter_list)).check(hasItemCount(3))
    }
  }

  @Test
  fun testStoryFragment_changeConfiguration_textViewIsShownCorrectly() {
    launch<StoryActivity>(createStoryActivityIntent()).use {
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      onView(allOf(withId(R.id.story_chapter_list))).perform(
        scrollToPosition<RecyclerView.ViewHolder>(
          1
        )
      )
      onView(atPositionOnView(R.id.story_chapter_list, 1, R.id.chapter_title)).check(
        matches(
          withText("Chapter 1: What is a Fraction?")
        )
      )
    }
  }

  @Test
  fun testStoryFragment_chapterSummaryIsShownCorrectly() {
    launch<StoryActivity>(createStoryActivityIntent()).use {
      testCoroutineDispatchers.runCurrent()
      onView(allOf(withId(R.id.story_chapter_list))).perform(
        scrollToPosition<RecyclerView.ViewHolder>(
          1
        )
      )
      onView(atPositionOnView(R.id.story_chapter_list, 1, R.id.chapter_summary)).check(
        matches(
          withText("This is outline/summary for What is a Fraction?")
        )
      )
    }
  }

  @Test
  fun testStoryFragment_changeConfiguration_chapterSummaryIsShownCorrectly() {
    launch<StoryActivity>(createStoryActivityIntent()).use {
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      onView(allOf(withId(R.id.story_chapter_list))).perform(
        scrollToPosition<RecyclerView.ViewHolder>(
          1
        )
      )
      onView(atPositionOnView(R.id.story_chapter_list, 1, R.id.chapter_summary)).check(
        matches(
          withText("This is outline/summary for What is a Fraction?")
        )
      )
    }
  }

  @Test
  fun testStoryFragment_changeConfiguration_explorationStartCorrectly() {
    launch<StoryActivity>(createStoryActivityIntent()).use {
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      onView(allOf(withId(R.id.story_chapter_list))).perform(
        scrollToPosition<RecyclerView.ViewHolder>(
          1
        )
      )
      onView(
        atPositionOnView(R.id.story_chapter_list, 1, R.id.story_chapter_card)
      ).perform(click())
      testCoroutineDispatchers.runCurrent()
      intended(hasComponent(ExplorationActivity::class.java.name))
    }
  }

  @Test
  fun testStoryFragment_changeConfiguration_correctStoryCountInHeader() {
    launch<StoryActivity>(createStoryActivityIntent()).use {
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      val headerString: String =
        getResources().getQuantityString(R.plurals.story_total_chapters, 2, 1, 2)
      onView(withId(R.id.story_chapter_list)).perform(
        scrollToPosition<RecyclerView.ViewHolder>(
          0
        )
      )
      onView(withId(R.id.story_chapter_list)).perform(
        scrollToPosition<RecyclerView.ViewHolder>(
          0
        )
      )
      onView(
        atPositionOnView(
          R.id.story_chapter_list,
          0,
          R.id.story_progress_chapter_completed_text
        )
      ).check(
        matches(
          withText(headerString)
        )
      )
    }
  }

  private fun setUpTestApplicationComponent() {
    ApplicationProvider.getApplicationContext<TestApplication>().inject(this)
  }

  private fun getResources(): Resources {
    return ApplicationProvider.getApplicationContext<Context>().resources
  }

  // TODO(#59): Figure out a way to reuse modules instead of needing to re-declare them.
  // TODO(#1675): Add NetworkModule once data module is migrated off of Moshi.
  @Singleton
  @Component(
    modules = [
      TestDispatcherModule::class, ApplicationModule::class,
      LoggerModule::class, ContinueModule::class, FractionInputModule::class,
      ItemSelectionInputModule::class, MultipleChoiceInputModule::class,
      NumberWithUnitsRuleModule::class, NumericInputRuleModule::class, TextInputRuleModule::class,
      DragDropSortInputModule::class, ImageClickInputModule::class, InteractionsModule::class,
      GcsResourceModule::class, GlideImageLoaderModule::class, ImageParsingModule::class,
      HtmlParserEntityTypeModule::class, QuestionModule::class, TestLogReportingModule::class,
      TestAccessibilityModule::class, LogStorageModule::class, CachingTestModule::class,
      PrimeTopicAssetsControllerModule::class, ExpirationMetaDataRetrieverModule::class,
      ViewBindingShimModule::class, RatioInputModule::class,
      ApplicationStartupListenerModule::class, LogUploadWorkerModule::class,
      WorkManagerConfigurationModule::class, HintsAndSolutionConfigModule::class,
      FirebaseLogUploaderModule::class
    ]
  )
  interface TestApplicationComponent : ApplicationComponent {
    @Component.Builder
    interface Builder : ApplicationComponent.Builder

    fun inject(storyFragmentTest: StoryFragmentTest)
  }

  class TestApplication : Application(), ActivityComponentFactory, ApplicationInjectorProvider {
    private val component: TestApplicationComponent by lazy {
      DaggerStoryFragmentTest_TestApplicationComponent.builder()
        .setApplication(this)
        .build() as TestApplicationComponent
    }

    fun inject(storyFragmentTest: StoryFragmentTest) {
      component.inject(storyFragmentTest)
    }

    override fun createActivityComponent(activity: AppCompatActivity): ActivityComponent {
      return component.getActivityComponentBuilderProvider().get().setActivity(activity).build()
    }

    override fun getApplicationInjector(): ApplicationInjector = component
  }
}
