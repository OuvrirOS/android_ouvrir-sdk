/**
 * Copyright (c) 2015, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ouvriros.tests.hardware.unit;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import ouvriros.app.OuvrirContextConstants;
import ouvriros.hardware.OuvrirHardwareManager;
import ouvriros.hardware.IOuvrirHardwareService;

/**
 * Created by adnan on 9/1/15.
 */
public class OuvrirHardwareManagerTest extends AndroidTestCase {
    private OuvrirHardwareManager mOuvrirHardwareManager;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Only run this if we support hardware abstraction
        org.junit.Assume.assumeTrue(mContext.getPackageManager().hasSystemFeature(
                OuvrirContextConstants.Features.HARDWARE_ABSTRACTION));
        mOuvrirHardwareManager = OuvrirHardwareManager.getInstance(mContext);
    }

    @SmallTest
    public void testManagerExists() {
        assertNotNull(mOuvrirHardwareManager);
    }

    @SmallTest
    public void testManagerServiceIsAvailable() {
        IOuvrirHardwareService iouvrirStatusBarManager = mOuvrirHardwareManager.getService();
        assertNotNull(iouvrirStatusBarManager);
    }
}
