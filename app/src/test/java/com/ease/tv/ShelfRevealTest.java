package com.ease.tv;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ShelfRevealTest {
 @Test public void stationaryVisibleTileNeedsNoScroll(){assertEquals(0,ShelfReveal.target(0,0,900,1500,2,290));}
 @Test public void nextOffscreenTileGetsNearestVisibleDestination(){assertEquals(280,ShelfReveal.target(0,0,900,1500,890,1180));}
 @Test public void newerFocusKeepsASuitableInFlightDestination(){assertEquals(280,ShelfReveal.target(40,280,900,1500,590,880));}
 @Test public void reversalBeforeFirstFrameCancelsOldDestination(){assertEquals(0,ShelfReveal.target(0,280,900,1500,2,290));}
 @Test public void reversalAfterScrollStartsRetargetsFromCurrentPosition(){assertEquals(2,ShelfReveal.target(75,280,900,1500,2,290));}
 @Test public void shrinkingContentClampsAnOldDestination(){assertEquals(100,ShelfReveal.target(280,280,900,1000,800,990));}
 @Test public void endMarginCannotScrollPastContent(){assertEquals(600,ShelfReveal.target(0,0,900,1500,1210,1510));}
 @Test public void shortShelfCannotScroll(){assertEquals(0,ShelfReveal.target(0,100,900,700,550,690));}
}
