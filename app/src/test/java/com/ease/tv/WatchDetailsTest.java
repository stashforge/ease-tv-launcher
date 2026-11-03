package com.ease.tv;
import org.junit.Test;
import static org.junit.Assert.*;
public class WatchDetailsTest {
 @Test public void combinesEpisodeAndRemainingTime(){assertEquals("S2 · E817 · 5 min left",WatchDetails.summary("2","817",1000,301000));}
 @Test public void roundsRemainingUp(){assertEquals("1 min left",WatchDetails.summary(null,null,1000,1001));}
 @Test public void formatsHours(){assertEquals("1 hr 4 min left",WatchDetails.summary(null,null,1000,3841000));}
 @Test public void omitsUnknownOrInvalidNumbers(){assertEquals("",WatchDetails.summary("Season 2","-1",0,0));}
 @Test public void omitsUnstartedAndFinishedTime(){assertEquals("E3",WatchDetails.summary(null,"3",0,90000));assertEquals("E3",WatchDetails.summary(null,"3",90000,90000));}
 @Test public void removesRepeatedShowTitle(){assertEquals("E3",WatchDetails.text("Show"," show ",null,"3",0,0));}
 @Test public void keepsDescriptiveEpisodeOnSecondLine(){assertEquals("E3 · 1 min left\nThe return",WatchDetails.text("Show","The return",null,"3",1000,61000));}
 @Test public void handlesMissingMetadata(){assertEquals("The return",WatchDetails.text("Show","The return",null,null,0,0));}
}
