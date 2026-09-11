package co.edu.konradlorenz.kapp.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.konradlorenz.kapp.R
import co.edu.konradlorenz.kapp.ui.theme.Border
import co.edu.konradlorenz.kapp.ui.theme.BorderSoft
import co.edu.konradlorenz.kapp.ui.theme.Brand
import co.edu.konradlorenz.kapp.ui.theme.InProgress
import co.edu.konradlorenz.kapp.ui.theme.KAppTheme
import co.edu.konradlorenz.kapp.ui.theme.Passed
import co.edu.konradlorenz.kapp.ui.theme.Person
import co.edu.konradlorenz.kapp.ui.theme.Placeholder
import co.edu.konradlorenz.kapp.ui.theme.Subject
import kotlin.math.roundToInt

// Every measurement below is read off docs/design/mobile/HomeAndroid.dc.html, which is drawn on a
// 360x800 canvas. The pixels there are dp here.
private val ScreenPadding = 16.dp
private val BandContentHeight = 80.dp
private val CardOverlap = 24.dp
private val HeadlineCardHeight = 166.dp
private val CardShape = RoundedCornerShape(18.dp)
private val RowShape = RoundedCornerShape(14.dp)
private val TileShape = RoundedCornerShape(16.dp)
private val BarShape = RoundedCornerShape(22.dp)

// The floating bar is 62 tall, sits 24 off the bottom and leaves 14 of air above it.
private val BarHeight = 62.dp
private val BarBottomInset = 24.dp
private val ContentBottomPadding = BarHeight + BarBottomInset + 14.dp

/**
 * Inicio.
 *
 * Nothing on this screen navigates. The four shortcuts, the two section links and the four other
 * tabs of the bottom bar all point at destinations that do not exist yet, so they are drawn and
 * left inert rather than wired to a route that would go nowhere - the same rule the login follows
 * for the states it cannot produce. Inicio is the only tab there is, which is why it is the one
 * drawn active.
 *
 * The data is [SampleHomeUiState] until there is a repository behind [HomeViewModel].
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    HomeContent(state = viewModel.uiState, modifier = modifier)
}

@Composable
private fun HomeContent(state: HomeUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // The band runs to the very top of the display and the system paints the status bar over
        // it, which is why MainActivity forces white status icons: they are always on purple.
        Band(student = state.student)

        // The band is 80 dp of content below the status bar, and the headline card climbs 24 dp
        // back into it. Starting the scrolling column 24 dp short of the band's edge puts the card
        // exactly where the mockup draws it, and makes the column the thing on top - so the card's
        // corners and its shadow land on the purple rather than behind it.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = BandContentHeight - CardOverlap)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = ScreenPadding,
                    end = ScreenPadding,
                    bottom = ContentBottomPadding,
                )
                .navigationBarsPadding(),
        ) {
            when (state.day) {
                DayState.Loading -> HeadlineSkeleton()
                DayState.NoSchedule -> NoScheduleCard()
                DayState.NoClassesToday -> FreeDayCard()
                is DayState.Classes -> NextClassCard(state.day.next)
            }

            Spacer(Modifier.height(16.dp))
            Shortcuts()

            SectionHeading(
                title = stringResource(R.string.home_semester_title),
                link = stringResource(R.string.home_semester_link),
            )
            when (state.semester) {
                SemesterState.Loading -> SemesterSkeleton()
                is SemesterState.Ready -> SemesterCard(state.semester)
            }

            // "Resto del dia" is the classes after the one on the card. With none of them there is
            // no heading either: an empty list under a title reads as something having failed.
            val later = (state.day as? DayState.Classes)?.later.orEmpty()
            if (later.isNotEmpty()) {
                SectionHeading(
                    title = stringResource(R.string.home_rest_title),
                    link = stringResource(R.string.home_rest_link),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    later.forEach { UpcomingRow(it) }
                }
            }
        }

        HomeBottomBar(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** The crest, the greeting and the initials, on one line over the brand purple. */
@Composable
private fun Band(student: Student) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brand)
            .statusBarsPadding()
            .height(BandContentHeight)
            .padding(horizontal = ScreenPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.konrad_logo),
                    contentDescription = stringResource(R.string.app_logo_description),
                    modifier = Modifier.size(30.dp),
                    // The crest keeps its own colours.
                    tint = Color.Unspecified,
                )
            }
            Text(
                text = stringResource(R.string.home_greeting, student.firstName),
                modifier = Modifier.weight(1f),
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = Color.White,
                maxLines = 1,
            )
            // Initials rather than a photo: user.openapi.yaml carries a name and no picture.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = student.initials,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * The card at the top of the screen, on its subject's colour.
 *
 * The rail colour is `ClassOccurrence.color`, which the API sends and the client only paints - see
 * the second note under "Pendientes de backend" in docs/design/mobile/README.md: if a subject can
 * come back pink, pink stops meaning "you can touch this" and the button below loses its meaning.
 */
@Composable
private fun NextClassCard(next: NextClass) {
    RailCard(rail = next.color, railWidth = 6.dp, shape = CardShape, elevation = 10.dp) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.home_next_class),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (next.startsInMinutes != null) {
                    // Purple on green, never white: the lime is 1.5:1 against it.
                    Text(
                        text = stringResource(R.string.home_next_starts_in, next.startsInMinutes),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(InProgress.copy(alpha = 0.32f))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Brand,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = next.courseName,
                style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.home_next_when_where,
                    next.startTime,
                    next.endTime,
                    next.room,
                    next.building,
                ),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            DirectionsButton()
        }
    }
}

/**
 * "Como llegar".
 *
 * Drawn, and inert: it belongs to map-service, which has no screen yet. It is the only pink thing
 * on the card, so it is the only thing on the card that will ever be touchable.
 */
@Composable
private fun DirectionsButton() {
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RowShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pin),
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = Color.White,
        )
        Text(
            text = stringResource(R.string.home_directions),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

/**
 * A day with a schedule and nothing on it.
 *
 * Same height as the card it replaces, so the four shortcuts under it do not move between one
 * student's screen and another's.
 */
@Composable
private fun FreeDayCard() {
    CenteredCard(height = HeadlineCardHeight) {
        IconBubble(R.drawable.ic_free_day, colour = InProgress, alpha = 0.30f, iconColour = Brand)
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.home_free_day_title),
            style = MaterialTheme.typography.titleMedium.copy(lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.home_free_day_link),
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * A student who has never built a schedule - `day` answers 404.
 *
 * Not an error: a 404 here means there is nothing to show yet, so the card asks for the missing
 * work instead of reporting a failure.
 */
@Composable
private fun NoScheduleCard() {
    CenteredCard(height = 208.dp) {
        IconBubble(R.drawable.ic_calendar_plus, colour = Subject, alpha = 0.16f)
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.home_no_schedule_title),
            style = MaterialTheme.typography.titleMedium.copy(lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.home_no_schedule_body),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RowShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.home_no_schedule_action),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

/** The headline card before schedule-service has answered. */
@Composable
private fun HeadlineSkeleton() {
    val loading = stringResource(R.string.home_loading)
    Panel(elevation = 10.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeadlineCardHeight)
                .padding(18.dp)
                // One label for the whole card: a screen reader has nothing to read out of four
                // grey bars.
                .semantics { contentDescription = loading },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBar(width = 104.dp, height = 11.dp, radius = 6.dp)
            SkeletonBar(width = 188.dp, height = 20.dp, radius = 7.dp)
            SkeletonBar(width = 236.dp, height = 13.dp, color = BorderSoft, radius = 6.dp)
            SkeletonBar(width = 142.dp, height = 44.dp, color = BorderSoft, radius = 14.dp)
        }
    }
}

/** Semaforo, Horario, Mapa and Perfil, one colour of the K each. */
@Composable
private fun Shortcuts() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Shortcut(R.drawable.ic_grid, Passed, R.string.home_shortcut_semaphore, 0.12f)
        Shortcut(R.drawable.ic_calendar, Subject, R.string.home_shortcut_schedule, 0.14f)
        Shortcut(R.drawable.ic_pin, Brand, R.string.home_shortcut_map, 0.12f)
        Shortcut(R.drawable.ic_person, Person, R.string.home_shortcut_profile, 0.12f)
    }
}

@Composable
private fun RowScope.Shortcut(
    @DrawableRes icon: Int,
    colour: Color,
    @StringRes label: Int,
    alpha: Float,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(TileShape)
                .background(colour.copy(alpha = alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                // The label below says the same thing.
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colour,
            )
        }
        Text(
            text = stringResource(label),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** A section title with its link on the right. 22 dp of air above it, 10 below. */
@Composable
private fun SectionHeading(title: String, link: String) {
    Spacer(Modifier.height(22.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = link,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Spacer(Modifier.height(10.dp))
}

/** Credits through the pensum: passed, being taken, and the rest of the track. */
@Composable
private fun SemesterCard(semester: SemesterState.Ready) {
    Panel(elevation = 2.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.home_semester_level, semester.level),
                        style = MaterialTheme.typography.labelLarge.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.home_semester_courses,
                            semester.coursesInProgress,
                            semester.coursesInProgress,
                        ),
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    // 78.9 is printed as 79: one decimal of a percentage is noise at this size.
                    text = stringResource(
                        R.string.home_semester_percent,
                        semester.percentComplete.roundToInt(),
                    ),
                    fontSize = 26.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Brand,
                )
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(semester)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.height(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Legend(Passed, R.plurals.home_credits_passed, semester.creditsPassed)
                Legend(InProgress, R.plurals.home_credits_in_progress, semester.creditsInProgress)
                Legend(Border, R.plurals.home_credits_remaining, semester.creditsRemaining)
            }
        }
    }
}

/**
 * Green for passed, lime for being taken, and the grey track showing through for the rest.
 *
 * The segments are weights rather than fractions of the width: inside a Row a second child asking
 * for a fraction would be measuring against what is left, not against the whole bar. A zero-credit
 * segment is left out, because a weight has to be greater than zero.
 */
@Composable
private fun ProgressBar(semester: SemesterState.Ready) {
    val passed = semester.passedFraction
    val inProgress = semester.inProgressFraction
    val rest = 1f - passed - inProgress
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Border),
    ) {
        if (passed > 0f) {
            Box(Modifier.weight(passed).fillMaxHeight().background(Passed))
        }
        if (inProgress > 0f) {
            Box(Modifier.weight(inProgress).fillMaxHeight().background(InProgress))
        }
        if (rest > 0f) {
            Spacer(Modifier.weight(rest))
        }
    }
}

@Composable
private fun Legend(colour: Color, @PluralsRes label: Int, credits: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(colour),
        )
        Text(
            text = pluralStringResource(label, credits, credits),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** The semester card before semaphore-service has answered. */
@Composable
private fun SemesterSkeleton() {
    val loading = stringResource(R.string.home_loading)
    Panel(elevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics { contentDescription = loading },
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SkeletonBar(width = 96.dp, height = 14.dp, radius = 7.dp)
                SkeletonBar(width = 54.dp, height = 22.dp, radius = 7.dp)
            }
            SkeletonBar(width = null, height = 10.dp, color = BorderSoft, radius = 5.dp)
            SkeletonBar(width = 220.dp, height = 11.dp, color = BorderSoft, radius = 6.dp)
        }
    }
}

/** One of the later classes of the day. */
@Composable
private fun UpcomingRow(upcoming: UpcomingClass) {
    RailCard(rail = upcoming.color, railWidth = 4.dp, shape = RowShape, elevation = 2.dp) {
        Row(
            modifier = Modifier
                .height(58.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.width(42.dp)) {
                Text(
                    text = upcoming.startTime,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = upcoming.endTime,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = Placeholder,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = upcoming.courseName,
                    style = MaterialTheme.typography.labelLarge.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(
                        R.string.home_class_where,
                        upcoming.room,
                        upcoming.building,
                    ),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The floating bar.
 *
 * It lives here rather than in a Scaffold because Inicio is the only destination that exists: a
 * navigation bar wired to one route is a picture of a navigation bar, and this is the place to
 * keep the picture until the other four screens are written.
 */
@Composable
private fun HomeBottomBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = ScreenPadding, end = ScreenPadding, bottom = BarBottomInset)
            .fillMaxWidth()
            .height(BarHeight)
            .shadow(12.dp, BarShape, ambientColor = Brand, spotColor = Brand)
            .clip(BarShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, BorderSoft, BarShape)
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab(R.drawable.ic_person, R.string.home_shortcut_profile)
        Tab(R.drawable.ic_grid, R.string.home_shortcut_semaphore)
        Tab(R.drawable.ic_home, R.string.home_tab_home, active = true)
        Tab(R.drawable.ic_pin, R.string.home_shortcut_map)
        Tab(R.drawable.ic_calendar, R.string.home_shortcut_schedule)
    }
}

/** The active tab carries Material's pill; the other four are the icon and its label. */
@Composable
private fun RowScope.Tab(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    active: Boolean = false,
) {
    val colour = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // The pill is 56 wide; an inactive tab is the same 32 dp box with nothing painted in it.
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(if (active) 56.dp else 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (active) colour.copy(alpha = 0.14f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colour,
            )
        }
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = colour,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Shapes the screen repeats.
// ---------------------------------------------------------------------------------------------

/** A white card with the shadow the mockup gives it. */
@Composable
private fun Panel(
    elevation: Dp,
    shape: RoundedCornerShape = CardShape,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation, shape, ambientColor = Brand, spotColor = Brand)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        content()
    }
}

/**
 * A card with a coloured rail down its left edge.
 *
 * `IntrinsicSize.Min` rather than a fixed height: the rail has to run the full height of whatever
 * the text turns out to be, and at the default font scale that height is the one the mockup draws.
 */
@Composable
private fun RailCard(
    rail: Color,
    railWidth: Dp,
    shape: RoundedCornerShape,
    elevation: Dp,
    content: @Composable () -> Unit,
) {
    Panel(elevation = elevation, shape = shape) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(railWidth)
                    .fillMaxHeight()
                    .background(rail),
            )
            content()
        }
    }
}

/** The two empty states: a bubble, a line or two, and something to do about it. */
@Composable
private fun CenteredCard(height: Dp, content: @Composable () -> Unit) {
    Panel(elevation = 10.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}

/**
 * A 48 dp disc of a colour at low opacity, with an icon in it.
 *
 * [iconColour] is separate because the free day draws a purple lamp on lime: white and lime are
 * 1.5:1, and so are lime on lime.
 */
@Composable
private fun IconBubble(
    @DrawableRes icon: Int,
    colour: Color,
    alpha: Float,
    iconColour: Color = colour,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colour.copy(alpha = alpha)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconColour,
        )
    }
}

/** One grey bar of a skeleton. A null width fills the row. */
@Composable
private fun SkeletonBar(
    width: Dp?,
    height: Dp,
    color: Color = Border,
    radius: Dp = height / 2,
) {
    val sizing = if (width == null) Modifier.fillMaxWidth() else Modifier.width(width)
    Box(
        sizing
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(color),
    )
}

// ---------------------------------------------------------------------------------------------
// The four states, at the size the mockups are drawn at.
// ---------------------------------------------------------------------------------------------

@Preview(name = "Inicio", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    KAppTheme { HomeContent(SampleHomeUiState) }
}

@Preview(name = "Inicio · cargando", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeLoadingPreview() {
    KAppTheme {
        HomeContent(
            SampleHomeUiState.copy(
                day = DayState.Loading,
                semester = SemesterState.Loading,
            ),
        )
    }
}

@Preview(name = "Inicio · hoy sin clases", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeFreeDayPreview() {
    KAppTheme { HomeContent(SampleHomeUiState.copy(day = DayState.NoClassesToday)) }
}

@Preview(name = "Inicio · sin horario", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeNoSchedulePreview() {
    KAppTheme { HomeContent(SampleHomeUiState.copy(day = DayState.NoSchedule)) }
}
