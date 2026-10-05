package dev.local.weatherstudy.ui.common.detail.state

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the per-card state classes in
 * com.samsung.android.weather.ui.common.detail.state:
 * `DetailHourlyCardState`/`DetailHourlyItemState`, `DetailDailyCardState`/`…ItemState`,
 * `DetailIndexCardState`/`…ItemState`, `DetailAirIndexCardState`/`…ItemState`,
 * `DetailSunCardState`, `DetailMoonCardState`, `DetailAlertCardState`/`…ItemState`,
 * `DetailInsightCardState`/`…ItemState`, `DetailLifeStyleCardState`/`…ItemState`,
 * `DetailLifeTipsCardState`/`…ItemState`, `DetailPrecipitationCardState`/`…ItemState`,
 * `DetailRadarCardState`, `DetailSmartThingsCardState`/`…ItemState`,
 * `DetailNewsCardState`, `DetailVideoCardState`, `DetailNewsAndVideoCardState`,
 * `DetailTodayStoriesAndVideoCardState`, `DetailBottomIndexCardState`
 *
 * ### The card/item split
 *
 * Nearly every card has **two** state classes — a `…CardState` and a `…ItemState`. The
 * card state is what the outer `ViewHolder` renders; the item state is one row of the
 * nested RecyclerView inside it. That mirrors the view structure exactly: 21 outer
 * holders, 15 inner holders, 10 inner adapters.
 *
 * All strings here are **already formatted**. The `usecase/notation` layer (27 classes
 * in the original) has run by the time state is built, so the ViewHolders do no
 * formatting and no unit conversion — which is why they can be as thin as they are.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
sealed interface StudyDetailCardState {
    val cardType: StudyDetailCardType
    val isVisible: Boolean
}

/** `DetailHourlyCardState` — hosts the bezier temperature curve. */
data class StudyDetailHourlyCardState(
    override val isVisible: Boolean = true,
    /**
     * The forecast sentence above the strip ("Scattered thunderstorms possible. Highs 30
     * to 32C and lows 22 to 24C.").
     *
     * It is NOT a card title. The original's `hourly_narrative` is a two-line
     * SizeLimitedTextView at Sec.600.White.13sp with a hairline divider under it; the
     * hourly card is the only card on the screen with no title at all. Gated on
     * `StudyWeatherPolicy.supportNarrative()`, because not every provider supplies one.
     */
    val narrative: String = "",
    val items: List<StudyDetailHourlyItemState> = emptyList(),
    val supportWind: Boolean = false,
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.Hourly
}

/**
 * `DetailHourlyItemState` — one hour.
 *
 * [temperatureRatio] is the field that makes [dev.local.weatherstudy.ui.common.detail.state.StudyDetailHourlyCardState]
 * work: the Canvas view draws its curve segment from the *normalised* value, and the
 * normalisation across the whole series happens here in the state provider, not in
 * `onDraw`. That is why each item view can draw only its own slice and still produce a
 * continuous curve.
 */
data class StudyDetailHourlyItemState(
    val timeText: String = "",
    val iconNum: Int = 0,
    val temperatureText: String = "",
    val temperatureRatio: Float = 0f,
    val previousRatio: Float = 0f,
    val nextRatio: Float = 0f,
    /** the curve's tangent at this hour and at its two neighbours, in ratio per item */
    val slope: Float = 0f,
    val previousSlope: Float = 0f,
    val nextSlope: Float = 0f,
    val precipitationText: String = "",
    val windText: String = "",
    val windDirectionDegree: Float = 0f,
    val isNow: Boolean = false,
    val isDay: Boolean = true,
)

/** `DetailDailyCardState`. */
data class StudyDetailDailyCardState(
    override val isVisible: Boolean = true,
    val items: List<StudyDetailDailyItemState> = emptyList(),
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.Daily
}

/** `DetailDailyItemState` — note the ratio pair, used for the high/low range bar. */
data class StudyDetailDailyItemState(
    val dayText: String = "",
    val dateText: String = "",
    val iconNum: Int = 0,
    val highText: String = "",
    val lowText: String = "",
    val highRatio: Float = 0f,
    val lowRatio: Float = 0f,
    val precipitationText: String = "",
    val isToday: Boolean = false,
)

/** `DetailIndexCardState` — the card hosting UV / humidity / pressure / wind / …. */
data class StudyDetailIndexCardState(
    override val isVisible: Boolean = true,
    val items: List<StudyDetailIndexItemState> = emptyList(),
    val isLargeLayout: Boolean = false,
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.Index
}

/**
 * `DetailIndexItemState` — one measurement.
 *
 * [graphValue] is normalised 0..1 for the Canvas views; [graphEntity] carries the
 * banding `GetIndexGraphViewEntity` computed.
 */
data class StudyDetailIndexItemState(
    val indexType: Int = 0,
    val titleText: String = "",
    val valueText: String = "",
    val levelText: String = "",
    val descriptionText: String = "",
    val graphValue: Float = 0f,
    val graphEntity: StudyIndexGraphViewEntity = StudyIndexGraphViewEntity(),
    /** meteorological degrees, for the wind compass only */
    val directionDegree: Float = 0f,
    val webUrl: String = "",
)

/**
 * Corresponds conceptually to `com.samsung.android.weather.ui.common.model.IndexGraphViewEntity`,
 * produced by `GetIndexGraphViewEntity` / `GetAqiGraphViewEntity` (+ the Chn/Jpn/Kor/Global
 * variants).
 *
 * Observed responsibility: the banding a Canvas graph paints — where the segments start
 * and stop, and which one the current value falls in. It is a use-case output rather
 * than a View constant because the bands are **regional**: four separate AQI variants
 * exist for exactly this reason.
 */
data class StudyIndexGraphViewEntity(
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    val bandBoundaries: List<Float> = emptyList(),
    val bandColors: List<Int> = emptyList(),
    val activeBandIndex: Int = 0,
)

/** `DetailAirIndexCardState` — hosts `AirQualityBar`. */
data class StudyDetailAirIndexCardState(
    override val isVisible: Boolean = true,
    val aqiText: String = "",
    val levelText: String = "",
    val scaleName: String = "",
    val graphEntity: StudyIndexGraphViewEntity = StudyIndexGraphViewEntity(),
    /** where the marker sits on the banded bar, 0..1 */
    val graphValue: Float = 0f,
    val pollutants: List<StudyDetailAirIndexItemState> = emptyList(),
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.AirIndex
}

/** `DetailAirIndexItemState`. */
data class StudyDetailAirIndexItemState(
    val nameText: String = "",
    val valueText: String = "",
    val levelText: String = "",
)

/**
 * `DetailSunCardState` and `DetailSunAltitudeState` — hosts `SunCurvedPathView`.
 *
 * [sunProgress] is the fraction of the way from sunrise to sunset, computed in the state
 * provider so the Canvas view only has to place a marker on its arc.
 */
data class StudyDetailSunCardState(
    override val isVisible: Boolean = true,
    val sunriseText: String = "",
    val sunsetText: String = "",
    val sunProgress: Float = 0f,
    val isPolarDay: Boolean = false,
    val isPolarNight: Boolean = false,
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.Sun
}

/** `DetailMoonCardState` and `DetailMoonPhaseState` — hosts `DetailMoonPhaseView`. */
data class StudyDetailMoonCardState(
    override val isVisible: Boolean = true,
    val moonriseText: String = "",
    val moonsetText: String = "",
    val phase: Int = 0,
    val phaseText: String = "",
    val illuminationFraction: Float = 0f,
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.Moon
}

/** The combined card — `detail_sun_and_moon_view_holder.xml` hosts both Canvas views. */
data class StudyDetailSunAndMoonCardState(
    override val isVisible: Boolean = true,
    val sun: StudyDetailSunCardState = StudyDetailSunCardState(),
    val moon: StudyDetailMoonCardState = StudyDetailMoonCardState(),
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.SunAndMoon
}

/** `DetailAlertCardState`. */
data class StudyDetailAlertCardState(
    override val isVisible: Boolean = false,
    val items: List<StudyDetailAlertItemState> = emptyList(),
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.Alert
}

/** `DetailAlertItemState`. */
data class StudyDetailAlertItemState(
    val titleText: String = "",
    val issuedText: String = "",
    val severityColor: Int = 0,
    val webUrl: String = "",
)

/** `DetailInsightCardState` — a pager of narrative cards. */
data class StudyDetailInsightCardState(
    override val isVisible: Boolean = false,
    val items: List<StudyDetailInsightItemState> = emptyList(),
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.Insight
}

/** `DetailInsightItemState`. */
data class StudyDetailInsightItemState(
    val insightType: Int = 0,
    val titleText: String = "",
    val contentText: String = "",
    val timeText: String = "",
    val iconNum: Int = 0,
    val webUrl: String = "",
)

/** `DetailPrecipitationCardState`. */
data class StudyDetailPrecipitationCardState(
    override val isVisible: Boolean = false,
    val summaryText: String = "",
    val unitText: String = "",
    val items: List<StudyDetailPrecipitationItemState> = emptyList(),
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.Precipitation
}

/** `DetailPrecipitationItemState`, with `DetailPrecipitationTypeState`/`…UnitState` folded in. */
data class StudyDetailPrecipitationItemState(
    val timeText: String = "",
    val amountText: String = "",
    val amountRatio: Float = 0f,
    val probabilityText: String = "",
    val precipitationType: Int = 0,
)

/** `DetailBottomIndexCardState`. */
data class StudyDetailBottomIndexCardState(
    override val isVisible: Boolean = true,
    val items: List<StudyDetailIndexItemState> = emptyList(),
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.BottomIndex
}

/** `DetailLifeStyleCardState`. */
data class StudyDetailLifeStyleCardState(
    override val isVisible: Boolean = false,
    val items: List<StudyDetailLifeStyleItemState> = emptyList(),
    val showSettingRow: Boolean = true,
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.LifeStyle
}

/** `DetailLifeStyleItemState` and `DetailLifeStyleItemStateKt`. */
data class StudyDetailLifeStyleItemState(
    val lifeStyleType: Int = 0,
    val titleText: String = "",
    val stateText: String = "",
    val descriptionText: String = "",
    val stateType: Int = 0,
    val bandCount: Int = 3,
    val statesByTime: List<StudyDetailLifeStyleByTimeState> = emptyList(),
)

/** backs `detail_life_style_inner_state_by_time_item.xml`. */
data class StudyDetailLifeStyleByTimeState(
    val timeText: String = "",
    val stateType: Int = 0,
)

/** `DetailLifeTipsCardState` / `DetailLifeTipsItemState`. */
data class StudyDetailLifeTipsCardState(
    override val isVisible: Boolean = false,
    val items: List<StudyDetailLifeTipsItemState> = emptyList(),
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.LifeTips
}

/** `DetailLifeTipsItemState`. */
data class StudyDetailLifeTipsItemState(
    val titleText: String = "",
    val contentText: String = "",
    val iconNum: Int = 0,
)

/** `DetailRadarCardState` — stubbed in the reconstruction; needs a Maps key. */
data class StudyDetailRadarCardState(
    override val isVisible: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val webUrl: String = "",
    val isMapAvailable: Boolean = false,
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.Radar
}

/** `DetailSmartThingsCardState` / `…ItemState` / `…DeviceState` / `…SummaryItemState`. */
data class StudyDetailSmartThingsCardState(
    override val isVisible: Boolean = false,
    val items: List<StudyDetailSmartThingsItemState> = emptyList(),
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.SmartThings
}

/** `DetailSmartThingsItemState`. */
data class StudyDetailSmartThingsItemState(
    val roomName: String = "",
    val summaryText: String = "",
    val devices: List<StudyDetailSmartThingsDeviceState> = emptyList(),
)

/** `DetailSmartThingsDeviceState`. */
data class StudyDetailSmartThingsDeviceState(
    val deviceName: String = "",
    val statusText: String = "",
    val iconNum: Int = 0,
)

/** `DetailNewsCardState`, `DetailVideoCardState`, `DetailNewsAndVideoCardState`,
 * `DetailTodayStoriesAndVideoCardState` — four card types over one content state. */
data class StudyDetailContentCardState(
    override val cardType: StudyDetailCardType,
    override val isVisible: Boolean = false,
    val items: List<StudyDetailContentItemState> = emptyList(),
    val moreUrl: String = "",
) : StudyDetailCardState

/** `DetailContentsItemState`. */
data class StudyDetailContentItemState(
    val titleText: String = "",
    val summaryText: String = "",
    val imageUrl: String = "",
    val webUrl: String = "",
    val contentType: Int = 0,
)

/** `DetailIndicatorState`'s card wrapper. */
data class StudyDetailIndicatorCardState(
    override val isVisible: Boolean = true,
    val indicator: StudyDetailIndicatorState = StudyDetailIndicatorState(),
) : StudyDetailCardState {
    override val cardType = StudyDetailCardType.Indicator
}
