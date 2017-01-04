/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017  Diversity Arrays Technology, Pty Ltd.
    
    KDXplore may be redistributed and may be modified under the terms
    of the GNU General Public License as published by the Free Software
    Foundation, either version 3 of the License, or (at your option)
    any later version.
    
    KDXplore is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with KDXplore.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.diversityarrays.kdxplore.trialmgr;

import java.awt.event.ActionEvent;
import java.util.function.Predicate;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.services.KdxPluginInfo;
import com.diversityarrays.kdxservice.IOfflineData;
import com.diversityarrays.kdxservice.KDXDeviceService;
import com.diversityarrays.kdxservice.KDXchangeService;
import com.diversityarrays.kdxservice.KDXchangeService.KDXchangeGui;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.desktop.WindowOpener;

public class ExplorerServices {

    private static final String TAG = "ExplorerServices"; //$NON-NLS-1$

    
    // Use these during development
    private static final String DEVICE_SERVICE_IMPL_CLASSNAME = "com.diversityarrays.device.KDXDeviceServiceImpl"; //$NON-NLS-1$
//    private static final String GENOTYPE_SERVICE_IMPL_CLASSNAME = "com.diversityarrays.kdxplore.gsearch.KDXGenotypeServiceImpl"; //$NON-NLS-1$
    private static final String KDXCHANGE_SERVICE_IMPL_CLASSNAME = "com.diversityarrays.kdxchange.KDXchangeServiceImpl"; //$NON-NLS-1$


    private KDXchangeService kdxChangeService;
//    private KDXGenotypeService kdxGenotypeService;
    private KDXDeviceService kdxDeviceService;


    private final WindowOpener<JFrame> frameWindowOpener;

    private final IOfflineData offlineData;

    
    private final Action kdxChangeServiceAction = new AbstractAction(Msg.ACTION_KDXCHANGE_SERVER()) {
        @Override
        public void actionPerformed(ActionEvent e) {

            JFrame frame = frameWindowOpener.getWindowByIdentifier(kdxChangeServiceAction);
            if (frame != null) {
                GuiUtil.restoreFrame(frame);
            }
            else {
                if (kdxChangeService == null) {
                    MsgBox.error(parentFrame, 
                            Msg.MSG_SERVICE_NOT_AVAILABLE(), 
                            msgTitle);
                }
                else {
                    doOpenKdxChangeService(kdxChangeService);
                }
            }
        }

        private void doOpenKdxChangeService(final KDXchangeService service) {
            try {
                KDXchangeGui gui = service.createUserInterface();
                gui.setOfflineData(offlineData);

                JFrame frame = gui.getJFrame();
                frame.setLocationRelativeTo(parentFrame);

                frame.setVisible(true);

                frameWindowOpener.addWindow(frame, frame.getTitle(), kdxChangeServiceAction);
            }
            catch (Throwable e) {
                Shared.Log.e(TAG, 
                        "doOpenKdxChangeService(" + service.getClass().getName() + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
                MsgBox.error(parentFrame,
                        Msg.MSG_UNABLE_TO_START_KDXCHANGE_SERVER_NAME_CAUSE(e.getClass().getName(), e.getMessage()),
                        msgTitle);
            }
        }
    };

//    private final Action genotypeDataAction = new AbstractAction(Msg.ACTION_GENOTYPE_EXPLORER()) {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//
//            JFrame frame = frameWindowOpener.getWindowByIdentifier(genotypeDataAction);
//            if (frame != null) {
//                GuiUtil.restoreFrame(frame);
//            }
//            else if (kdxGenotypeService == null) {
//                MsgBox.error(parentFrame, 
//                        Msg.ERRMSG_THIS_FUNCTION_NOT_AVAILABLE(), 
//                        Msg.ACTION_GENOTYPE_EXPLORER());
//            }
//            else {
//                doOpenGenotypeService();
//            }
//        }
//        
//        private void doOpenGenotypeService() {
//            try {
//                JFrame frame = kdxGenotypeService.createUserInterface(clientProvider);
//
//                frame.setLocationRelativeTo(parentFrame);
//                frame.setVisible(true);
//                frameWindowOpener.addWindow(frame, frame.getTitle(), genotypeDataAction);
//
//                frame.toFront();
//            }
//            catch (Throwable e) {
//                Shared.Log.e(TAG, 
//                        "doOpenGenotypeService: " + kdxGenotypeService.getClass().getName(), e); //$NON-NLS-1$
//                MsgBox.error(parentFrame,
//                        Msg.MSG_UNABLE_TO_START_GENOTYPE_EXPLORER_NAME_CAUSE(e.getClass().getName(), e.getMessage()),
//                        msgTitle);
//            }
//        }
//    };

    private final Action deviceServiceAction = new AbstractAction(Msg.ACTION_DEVICE_SERVER()) {

        @Override
        public void actionPerformed(ActionEvent e) {

            // TODO figure out who provided the deviceServiceAction as the
            // identifier
            // because frameWindowOpener never got called to add it in
            JFrame frame = frameWindowOpener.getWindowByIdentifier(deviceServiceAction);

            if (frame != null) {
                GuiUtil.restoreFrame(frame);
            }
            else if (kdxDeviceService == null) {
                MsgBox.info(parentFrame,
                        Msg.MSG_SERVICE_NOT_AVAILABLE(),
                        msgTitle);
            }
            else {
                doOpenDeviceService(kdxDeviceService);
            }
        }

        private void doOpenDeviceService(final KDXDeviceService service) {
            try {
                final JFrame frame = service.createUserInterface();
                frame.setLocationRelativeTo(parentFrame);
                frame.setVisible(true);

                frameWindowOpener.addWindow(frame, frame.getTitle(), deviceServiceAction);
            }
            catch (Throwable e) {
                Shared.Log.e(TAG, "doOpenDeviceService: " + service.getClass().getName(), e); //$NON-NLS-1$
                MsgBox.error(parentFrame,
                        Msg.MSG_UNABLE_TO_START_DEVICE_SERVER_NAME_CAUSE(e.getClass().getName(), e.getMessage()),
                        msgTitle);
            }
        }
    };
    
    private final JFrame parentFrame;
    private final String msgTitle;
//    private final DALClientProvider clientProvider;
    
    public ExplorerServices(
            KdxPluginInfo pluginInfo,
            IOfflineData offlineData
//            , DALClientProvider clientProvider
            )
    {
        this.parentFrame = pluginInfo.getKdxploreFrame();
        this.msgTitle = parentFrame.getTitle();
        this.frameWindowOpener = pluginInfo.getWindowOpener();
        this.offlineData = offlineData;
//        this.clientProvider = clientProvider;

        KDClientUtils.initAction(ImageId.KDEXCHANGE, 
                kdxChangeServiceAction, 
                Msg.TOOLTIP_DISPLAY_KDXCHANGE_SERVER(),
                false);
//        KDClientUtils.initAction(ImageId.EXPLORE_GENO_DATA, 
//                genotypeDataAction,
//                Msg.TOOLTIP_DISPLAY_GENOTYPE_EXPLORER(), 
//                false);
        KDClientUtils.initAction(ImageId.DEVICE_SERVER, 
                deviceServiceAction, 
                Msg.TOOLTIP_DISPLAY_DEVICE_SERVER(),
                false);
        
        Predicate<KDXchangeService> onServiceFound = new Predicate<KDXchangeService>() {
            @Override
            public boolean test(KDXchangeService s) {
                kdxChangeService = s;
                return false; // only want the first
            }
        };
        
        // TODO detect and report multiple
        Shared.detectServices(KDXchangeService.class, onServiceFound, KDXCHANGE_SERVICE_IMPL_CLASSNAME);
        kdxChangeServiceAction.setEnabled(kdxChangeService != null);
        
//        boolean cimmytMode = KdxploreConfig.getInstance().isCIMMYTmode();
//
//        if (cimmytMode) {
//            genotypeDataAction.setEnabled(false); // yes, this is deliberate
//        }
//        else {
//            // TODO detect and report multiple
//            
//            if (RunMode.DEMO == RunMode.getRunMode()) {
//                // until we figure out why we get an empty panel
//                genotypeDataAction.setEnabled(false);
//            }
//            else {
//                Predicate<KDXGenotypeService> onGSfound = new Predicate<KDXGenotypeService>() {
//                    @Override
//                    public boolean test(KDXGenotypeService s) {
//                        kdxGenotypeService = s;
//                        return false; // only want the first
//                    }
//                };
//                Shared.detectServices(KDXGenotypeService.class, onGSfound, GENOTYPE_SERVICE_IMPL_CLASSNAME);
//                genotypeDataAction.setEnabled(kdxGenotypeService != null);
//            }
//
//            // - - - - - - - - - - -
//        }

        // TODO detect and report multiple
        Predicate<KDXDeviceService> onDSfound = new Predicate<KDXDeviceService>() {
            @Override
            public boolean test(KDXDeviceService s) {
                kdxDeviceService = s;
                return false; // only want the first
            }
        };
        Shared.detectServices(KDXDeviceService.class, onDSfound, DEVICE_SERVICE_IMPL_CLASSNAME);
        deviceServiceAction.setEnabled(kdxDeviceService != null);
    }

    public KDXDeviceService getKdxDeviceService() {
        return kdxDeviceService;
    }

    public void addActions(Box box) {
        Action[] actions = new Action[] {
                kdxChangeServiceAction,
//                genotypeDataAction,
                deviceServiceAction,
        };
        for (Action a : actions) {
            box.add(new JButton(a));
//            if (a.isEnabled()) {
//                box.add(new JButton(a));
//            }
        }
    }
    
}
