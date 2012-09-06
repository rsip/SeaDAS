package org.esa.beam.watermask.ui;

import com.bc.ceres.core.*;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.jidesoft.action.CommandBar;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandAdapter;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.AbstractVisatPlugIn;
import org.esa.beam.visat.VisatApp;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.FormatDescriptor;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: knowles
 * Date: 9/6/12
 * Time: 7:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class NewWaterMaskVPI extends AbstractVisatPlugIn {

    public static final String COMMAND_ID = "newCreateLandWaterCoastMasks";

    public static final String LAND_WATER_MASK_OP_ALIAS = "LandWaterMask";
    public static final String TARGET_TOOL_BAR_NAME = "layersToolBar";
    public static final String WATER_FRACTION_BAND_NAME = "mask_data_water_fraction";
    public static final String WATER_FRACTION_SMOOTHED_NAME = "mask_data_water_fraction_smoothed";


    private static final String LAND_MASK_NAME = "LandMask";
    private static final String LAND_MASK_MATH = WATER_FRACTION_BAND_NAME + " == 0";
    private static final String LAND_MASK_DESCRIPTION = "Land pixels";


    private static final String COASTLINE_MASK_NAME = "CoastLine";
    private static final String COASTLINE_MATH = WATER_FRACTION_SMOOTHED_NAME + " > 25 and " + WATER_FRACTION_SMOOTHED_NAME + " < 75";
    private static final String COASTLINE_MASK_DESCRIPTION = "Coastline pixels";


    private static final String WATER_MASK_NAME = "WaterMask";
    private static final String WATER_MASK_MATH = WATER_FRACTION_BAND_NAME + " > 0";
    private static final String WATER_MASK_DESCRIPTION = "Water pixels";


    @Override
    public void start(final VisatApp visatApp) {
        final ExecCommand action = visatApp.getCommandManager().createExecCommand(COMMAND_ID,
                new ToolbarCommand(visatApp));
        //  action.setLargeIcon(UIUtils.loadImageIcon("/org/esa/beam/watermask/ui/icons/dock.gif"));
        action.setLargeIcon(UIUtils.loadImageIcon("/org/esa/beam/watermask/ui/icons/coastline2_24.png"));

        final AbstractButton lwcButton = visatApp.createToolButton(COMMAND_ID);

        visatApp.getMainFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                CommandBar layersBar = visatApp.getToolBar(TARGET_TOOL_BAR_NAME);
                layersBar.add(lwcButton);
            }

        });
    }


    private void showLandWaterCoastMasks(final VisatApp visatApp) {

        final AuxilliaryMasksData auxilliaryMasksData = new AuxilliaryMasksData();


        // BEAM COMMENTS
//
//        JPanel lwcPanel = GridBagUtils.createPanel();
//        JPanel coastlinePanel = GridBagUtils.createPanel();
//        GridBagConstraints coastlineConstraints = new GridBagConstraints();
//        int rightInset = 10;
//
//        SpinnerModel transparencyModel = new SpinnerNumberModel(0.4, 0.0, 1.0, 0.1);
//        JSpinner transparencySpinner = new JSpinner(transparencyModel);
//
//        SpinnerModel samplingModel = new SpinnerNumberModel(1, 1, 10, 1);
//        JSpinner xSamplingSpinner = new JSpinner(samplingModel);
//        JSpinner ySamplingSpinner = new JSpinner(samplingModel);
//
//        Integer[] resolutions = {50, 150};
//        JComboBox resolutionComboBox = new JComboBox(resolutions);
//
//        GridBagUtils.addToPanel(coastlinePanel, new JCheckBox("Coastline"), coastlineConstraints, "anchor=WEST, gridx=0, gridy=0");
//        GridBagUtils.addToPanel(coastlinePanel, new JLabel("Mask name: "), coastlineConstraints, "gridy=1, insets.right="+ rightInset);
//        GridBagUtils.addToPanel(coastlinePanel, new JTextField("Coastline"), coastlineConstraints, "gridx=1, insets.right=0");
//        GridBagUtils.addToPanel(coastlinePanel, new JLabel("Line color: "), coastlineConstraints, "gridx=0, gridy=2, insets.right=" + rightInset);
//        GridBagUtils.addToPanel(coastlinePanel, new ColorExComboBox(), coastlineConstraints, "gridx=1, insets.right=0");
//        GridBagUtils.addToPanel(coastlinePanel, new JLabel("Transparency: "), coastlineConstraints, "gridy=2, insets.right="+ rightInset);
//        GridBagUtils.addToPanel(coastlinePanel, transparencySpinner, coastlineConstraints, "gridx=1, insets.right=0");
//        GridBagUtils.addToPanel(coastlinePanel, new JLabel("Resolution: "), coastlineConstraints, "gridy=3, insets.right="+ rightInset);
//        GridBagUtils.addToPanel(coastlinePanel, resolutionComboBox, coastlineConstraints, "gridx=1, insets.right=0");
//        GridBagUtils.addToPanel(coastlinePanel, new JLabel("Supersampling factor x: "), coastlineConstraints, "gridy=4, insets.right="+ rightInset);
//        GridBagUtils.addToPanel(coastlinePanel, xSamplingSpinner, coastlineConstraints, "gridx=1, insets.right=0");
//        GridBagUtils.addToPanel(coastlinePanel, new JLabel("Supersampling factor y: "), coastlineConstraints, "gridy=5, insets.right="+ rightInset);
//        GridBagUtils.addToPanel(coastlinePanel, ySamplingSpinner, coastlineConstraints, "gridx=1, insets.right=0");


        final Product product = visatApp.getSelectedProduct();
        if (product != null) {


            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();

            final boolean[] masksCreated = {false};
//
//            for (String name : maskGroup.getNodeNames()) {
//
//                System.out.println("mask name=" + name);
//                if (name.equals(COASTLINE_MASK_NAME) || name.equals(LAND_MASK_NAME) || name.equals(WATER_MASK_NAME)) {
//                   maskGroup.remove(maskGroup.get(name));
//                }
//            }
//
//            System.out.println("list after removal of masks");
//            for (String name : maskGroup.getNodeNames()) {
//
//                System.out.println("mask name=" + name);
//                if (name.equals(COASTLINE_MASK_NAME) || name.equals(LAND_MASK_NAME) || name.equals(WATER_MASK_NAME)) {
//                    masksCreated[0] = true;
//                }
//            }
//
//
//            final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
//
//
//            for (String name : bandGroup.getNodeNames()) {
//                System.out.println("band name=" + name);
//                if (name.equals(WATER_FRACTION_BAND_NAME) || name.equals(WATER_FRACTION_SMOOTHED_NAME)) {
//                    System.out.println(name+" needs to be deleted");
//                    bandGroup.remove(bandGroup.get(name));
//                }
//            }
//
//            System.out.println("list after removal of masks");
//
//            for (String name : bandGroup.getNodeNames()) {
//                System.out.println("band name=" + name);
//                if (name.equals(WATER_FRACTION_BAND_NAME) || name.equals(WATER_FRACTION_SMOOTHED_NAME)) {
//                    System.out.println(name+" needs to be deleted");
//
//                }
//            }





            AuxilliaryMasksDialog auxilliaryMasksDialog = new AuxilliaryMasksDialog(auxilliaryMasksData, masksCreated[0]);
            auxilliaryMasksDialog.setVisible(true);
            System.out.println("superSampling=" + auxilliaryMasksData.getSuperSampling());

            if (!masksCreated[0] && auxilliaryMasksData.isCreateMasks()) {
                ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(visatApp.getMainFrame(),
                        "Computing Masks") {

                    @Override
                    protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                        pm.beginTask("Creating land, water, coastline masks", 2);

                        try {
//        Product landWaterProduct = GPF.createProduct("LandWaterMask", GPF.NO_PARAMS, product);


                            Map<String, Object> parameters = new HashMap<String, Object>();

//                            parameters.put("subSamplingFactorX", new Integer(auxilliaryMasksData.getSuperSampling()));
//                            parameters.put("subSamplingFactorY", new Integer(auxilliaryMasksData.getSuperSampling()));

                            parameters.put("subSamplingFactorX", 3);
                            parameters.put("subSamplingFactorY", 3);


                            //    parameters.put("resolution", (Object) auxilliaryMasksData.getResolution());
                            Product landWaterProduct = GPF.createProduct(LAND_WATER_MASK_OP_ALIAS, parameters, product);
                            Band waterFractionBand = landWaterProduct.getBand("land_water_fraction");
                            Band coastBand = landWaterProduct.getBand("coast");

                            // PROBLEM WITH TILE SIZES
                            // Example: product has tileWidth=498 and tileHeight=611
                            // resulting image has tileWidth=408 and tileHeight=612
                            // Why is this happening and where?
                            // For now we change the image layout here.
                            reformatSourceImage(waterFractionBand, new ImageLayout(product.getBandAt(0).getSourceImage()));
                            reformatSourceImage(coastBand, new ImageLayout(product.getBandAt(0).getSourceImage()));

                            pm.worked(1);
                            waterFractionBand.setName(WATER_FRACTION_BAND_NAME);
                            product.addBand(waterFractionBand);
                            //todo BEAM folks left this as a placeholder
//                    product.addBand(coastBand);

                            //todo replace with JAI operator "GeneralFilter" which uses a GeneralFilterFunction
                            final Kernel arithmeticMean3x3Kernel = new Kernel(3, 3, 1.0 / 9.0,
                                    new double[]{
                                            +1, +1, +1,
                                            +1, +1, +1,
                                            +1, +1, +1,
                                    });
                            final ConvolutionFilterBand filteredCoastlineBand = new ConvolutionFilterBand(WATER_FRACTION_SMOOTHED_NAME,
                                    waterFractionBand,
                                    arithmeticMean3x3Kernel);
                            product.addBand(filteredCoastlineBand);

                            //   ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();


                            Mask coastlineMask = Mask.BandMathsType.create(COASTLINE_MASK_NAME,
                                    COASTLINE_MASK_DESCRIPTION,
                                    product.getSceneRasterWidth(),
                                    product.getSceneRasterHeight(),
                                    COASTLINE_MATH,
                                    auxilliaryMasksData.getCoastlineMaskColor(),
                                    auxilliaryMasksData.getCoastlineMaskTransparency());
                            maskGroup.add(coastlineMask);


                            Mask waterMask = Mask.BandMathsType.create(WATER_MASK_NAME,
                                    WATER_MASK_DESCRIPTION,
                                    product.getSceneRasterWidth(),
                                    product.getSceneRasterHeight(),
                                    WATER_MASK_MATH,
                                    auxilliaryMasksData.getWaterMaskColor(),
                                    auxilliaryMasksData.getWaterMaskTransparency());
                            maskGroup.add(waterMask);


                            Mask landMask = Mask.BandMathsType.create(LAND_MASK_NAME,
                                    LAND_MASK_DESCRIPTION,
                                    product.getSceneRasterWidth(),
                                    product.getSceneRasterHeight(),
                                    LAND_MASK_MATH,
                                    auxilliaryMasksData.getLandMaskColor(),
                                    auxilliaryMasksData.getLandMaskTransparency());
//                    Color.GREEN.darker(),

                            maskGroup.add(landMask);


                            pm.worked(1);

                            String[] bandNames = product.getBandNames();
                            for (String bandName : bandNames) {
                                RasterDataNode raster = product.getRasterDataNode(bandName);
                                if (auxilliaryMasksData.isShowCoastlineMaskAllBands()) {
                                    raster.getOverlayMaskGroup().add(coastlineMask);
                                }
                                if (auxilliaryMasksData.isShowLandMaskAllBands()) {
                                    raster.getOverlayMaskGroup().add(landMask);
                                }
                                if (auxilliaryMasksData.isShowWaterMaskAllBands()) {
                                    raster.getOverlayMaskGroup().add(waterMask);
                                }
                            }


//                    visatApp.setSelectedProductNode(waterFractionBand);

//        ProductSceneView selectedProductSceneView = visatApp.getSelectedProductSceneView();
//        if (selectedProductSceneView != null) {
//            RasterDataNode raster = selectedProductSceneView.getRaster();
//            raster.getOverlayMaskGroup().add(landMask);
//            raster.getOverlayMaskGroup().add(coastlineMask);
//            raster.getOverlayMaskGroup().add(waterMask);

//        }


                        } finally {
                            pm.done();
                        }
                        return null;
                    }


                };

                pmSwingWorker.executeWithBlocking();
            }
        }

    }


    private void reformatSourceImage(Band band, ImageLayout imageLayout) {
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        MultiLevelImage waterFractionSourceImage = band.getSourceImage();
        int waterFractionDataType = waterFractionSourceImage.getData().getDataBuffer().getDataType();
        RenderedImage newImage = FormatDescriptor.create(waterFractionSourceImage, waterFractionDataType,
                renderingHints);
        band.setSourceImage(newImage);
    }

    private class ToolbarCommand extends CommandAdapter {
        private final VisatApp visatApp;

        public ToolbarCommand(VisatApp visatApp) {
            this.visatApp = visatApp;
        }

        @Override
        public void actionPerformed(
                CommandEvent event) {
            showLandWaterCoastMasks(
                    visatApp);

        }

        @Override
        public void updateState(
                CommandEvent event) {
            Product selectedProduct = visatApp.getSelectedProduct();
            boolean productSelected = selectedProduct != null;
            boolean hasBands = false;
            boolean hasGeoCoding = false;
            if (productSelected) {
                hasBands = selectedProduct.getNumBands() > 0;
                hasGeoCoding = selectedProduct.getGeoCoding() != null;
            }
            event.getCommand().setEnabled(
                    productSelected && hasBands && hasGeoCoding);
        }
    }
}

