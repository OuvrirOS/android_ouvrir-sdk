package org.ouvriros.tests.hardware.unit;

import android.content.Context;
import android.os.PowerManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Assume;

import ouvriros.app.OuvrirContextConstants;
import ouvriros.hardware.OuvrirHardwareManager;
import ouvriros.hardware.ILiveDisplayService;
import ouvriros.hardware.LiveDisplayConfig;
import ouvriros.hardware.LiveDisplayManager;
import ouvriros.util.ColorUtils;

public class LiveDisplayManagerTest extends AndroidTestCase {

    private static final String TAG = "LiveDisplayManagerTest";

    private LiveDisplayManager mLiveDisplay;
    private OuvrirHardwareManager mHardware;

    private PowerManager mPower;
    private PowerManager.WakeLock mWakeLock;

    private LiveDisplayConfig mConfig;
    private int mInitialMode;

    @SuppressWarnings("deprecation")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Assume.assumeTrue(mContext.getPackageManager().hasSystemFeature(
                OuvrirContextConstants.Features.LIVEDISPLAY));

        mLiveDisplay = LiveDisplayManager.getInstance(mContext);
        if (mLiveDisplay.getConfig().hasModeSupport()) {
            mInitialMode = mLiveDisplay.getMode();
        }
        mConfig = mLiveDisplay.getConfig();

        mHardware = OuvrirHardwareManager.getInstance(mContext);
        mPower = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPower.newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        mWakeLock.acquire();
    }

    @Override
    protected void tearDown() {
        mLiveDisplay.setMode(mInitialMode);
        mWakeLock.release();
    }

    @SmallTest
    public void testManagerExists() {
        assertNotNull(mLiveDisplay);
    }

    @SmallTest
    public void testManagerServiceIsAvailable() {
        ILiveDisplayService service = LiveDisplayManager.getService();
        assertNotNull(service);
    }

    @SmallTest
    public void testConfig() {
        assertNotNull(mConfig);

        // at least GPU mode should be available
        assertTrue(mConfig.isAvailable());
    }

    @SmallTest
    public void testNightMode() throws Exception {
        Assume.assumeTrue(mConfig.hasModeSupport());

        int day = mLiveDisplay.getDayColorTemperature();
        int night = mLiveDisplay.getNightColorTemperature();

        mLiveDisplay.setMode(LiveDisplayManager.MODE_NIGHT);
        assertColorTemperature(night);

        // custom value
        mLiveDisplay.setNightColorTemperature(3300);
        assertColorTemperature(3300);

        // "default"
        mLiveDisplay.setNightColorTemperature(mConfig.getDefaultNightTemperature());
        assertColorTemperature(mConfig.getDefaultNightTemperature());

        mLiveDisplay.setNightColorTemperature(night);

        // day should not have changed
        assertEquals(day, mLiveDisplay.getDayColorTemperature());
    }

    @SmallTest
    public void testDayMode() throws Exception {
        Assume.assumeTrue(mConfig.hasModeSupport());

        int day = mLiveDisplay.getDayColorTemperature();
        int night = mLiveDisplay.getNightColorTemperature();

        mLiveDisplay.setMode(LiveDisplayManager.MODE_DAY);
        assertColorTemperature(day);

        // custom value
        mLiveDisplay.setDayColorTemperature(8000);
        assertColorTemperature(8000);

        // "default"
        mLiveDisplay.setDayColorTemperature(mConfig.getDefaultDayTemperature());
        assertColorTemperature(mConfig.getDefaultDayTemperature());

        mLiveDisplay.setDayColorTemperature(day);

        // night should not have changed
        assertEquals(night, mLiveDisplay.getNightColorTemperature());
    }

    @SmallTest
    public void testOutdoorMode() throws Exception {
        Assume.assumeTrue(mConfig.hasFeature(LiveDisplayManager.MODE_OUTDOOR));

        assertTrue(mHardware.isSupported(OuvrirHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT));

        mLiveDisplay.setMode(LiveDisplayManager.MODE_OUTDOOR);
        Thread.sleep(1000);
        assertTrue(mHardware.get(OuvrirHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT));

        mLiveDisplay.setMode(LiveDisplayManager.MODE_OFF);
        Thread.sleep(1000);
        assertFalse(mHardware.get(OuvrirHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT));
    }

    private void assertColorTemperature(int degK) throws Exception {
        Thread.sleep(2000);
        assertEquals(degK, LiveDisplayManager.getService().getColorTemperature());
        checkHardwareValue(ColorUtils.temperatureToRGB(degK));
    }

    private void checkHardwareValue(float[] expected) {
        int[] hardware = mHardware.getDisplayColorCalibration();
        int max = mHardware.getDisplayColorCalibrationMax();
        assertEquals((int)Math.floor(expected[0] * max), hardware[0]);
        assertEquals((int)Math.floor(expected[1] * max), hardware[1]);
        assertEquals((int)Math.floor(expected[2] * max), hardware[2]);
    }
}
