package org.oppia.android.app.home.topiclist

import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.oppia.android.app.home.RouteToTopicPlayStoryListener
import org.oppia.android.app.model.PromotedStory
import org.oppia.android.app.shim.IntentFactoryShim
import org.oppia.android.app.viewmodel.ObservableViewModel

// TODO(#283): Add download status information to promoted-story-card.

/** [ViewModel] for displaying a promoted story. */
class PromotedStoryViewModel(
  private val activity: AppCompatActivity,
  private val internalProfileId: Int,
  val entityType: String,
  private val IntentFactoryShim: IntentFactoryShim
) :
  ObservableViewModel(),
  RouteToTopicPlayStoryListener {

  /**
   * The retrieved [LiveData] for retrieving topic summaries. This model should ensure only one
   * [LiveData] is used for all subsequent processed data to ensure the transformed [LiveData]s are
   * always in sync.
   */
  val promotedStoryObservable = ObservableField<PromotedStory>()

  fun setPromotedStory(promotedStory: PromotedStory) {
    promotedStoryObservable.set(promotedStory)
  }

  fun clickOnStoryTile() {
    routeToTopicPlayStory(
      internalProfileId,
      promotedStoryObservable.get()!!.topicId,
      promotedStoryObservable.get()!!.storyId
    )
  }

  override fun routeToTopicPlayStory(internalProfileId: Int, topicId: String, storyId: String) {
    val intent = IntentFactoryShim.createTopicPlayStoryActivityIntent(
      activity.applicationContext,
      internalProfileId,
      topicId,
      storyId
    )
    activity.startActivity(intent)
  }
}
