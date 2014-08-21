/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.math.Range;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

class Continuous1BandBasicForm implements ColorManipulationChildForm {

    private final ColorManipulationForm parentForm;
    private final JPanel contentPanel;
    private final AbstractButton logDisplayButton;
    private final MoreOptionsForm moreOptionsForm;
    private final ColorPaletteChooser colorPaletteChooser;
    private JFormattedTextField minField;
    private JFormattedTextField maxField;
    private final DiscreteCheckBox discreteCheckBox;
    private final JCheckBox loadWithCPDFileValuesCheckBox;
    private final ColorPaletteSchemes colorPaletteSchemes;
    private JButton bandRange;
    private JLabel colorScheme;
    private JLabel colorPalette;


    final Boolean[] minFieldActivated = {new Boolean(false)};
    final Boolean[] maxFieldActivated = {new Boolean(false)};
    final Boolean[] listenToLogDisplayButtonEnabled = {true};
    final Boolean[] basicSwitcherIsActive;


    private final ImageInfoEditor2 imageInfoEditor;

    private enum RangeKey {FromPaletteSource, FromData, FromMinMaxFields, FromCurrentPalette, ToggleLog;}

    private boolean shouldFireChooserEvent;
    private boolean hidden = false;

    Continuous1BandBasicForm(final ColorManipulationForm parentForm, final Boolean[] basicSwitcherIsActive) {

        imageInfoEditor = new ImageInfoEditor2(parentForm);

        ColorPalettesManager.loadAvailableColorPalettes(parentForm.getIODir());

        this.parentForm = parentForm;
        this.basicSwitcherIsActive = basicSwitcherIsActive;

        colorScheme = new JLabel("");
        colorScheme.setToolTipText("Last loaded scheme.  Astericks suffix (*) denotes that user may have modified parameters");
        colorPalette = new JLabel("");


        colorPaletteSchemes = new ColorPaletteSchemes(parentForm.getIODir());
        VisatApp.getApp().clearStatusBarMessage();

        loadWithCPDFileValuesCheckBox = new JCheckBox("Load with exact file values", false);
        loadWithCPDFileValuesCheckBox.setToolTipText("When loading a new cpd file, use it's actual value and overwrite user min/max values");


        colorPaletteChooser = new ColorPaletteChooser();
        colorPaletteChooser.setPreferredSize(new Dimension(180, 40));
        colorPaletteChooser.setMinimumSize(new Dimension(180, 60));
        colorPaletteChooser.setMaximumRowCount(20);


        JPanel colorPaletteJPanel = getColorPaletteFilePanel("Palettes");
        JPanel rangeJPanel = getRangePanel("Range Adjustments");
        JPanel colorPaletteInfoComboBoxJPanel = getSchemaPanel("Schemes");


        JPanel basicPanel = GridBagUtils.createPanel();
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.gridx = 0;
        basicPanel.add(spacer1(new JLabel()), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        basicPanel.add(colorPaletteInfoComboBoxJPanel, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        basicPanel.add(spacer1(new JLabel()), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        basicPanel.add(colorPaletteJPanel, gbc);


        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        basicPanel.add(spacer1(new JLabel()), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        basicPanel.add(rangeJPanel, gbc);


        contentPanel = GridBagUtils.createPanel();

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weighty = 1.0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(basicPanel, gbc);


        shouldFireChooserEvent = true;

        colorPaletteChooser.addActionListener(createListener(RangeKey.FromCurrentPalette));
//        minField.addActionListener(createListener(RangeKey.FromMinMaxFields));
//        maxField.addActionListener(createListener(RangeKey.FromMinMaxFields));
        //       cpdRange.addActionListener(createListener(RangeKey.FromPaletteSource));
        bandRange.addActionListener(createListener(RangeKey.FromData));


        colorPaletteChooser.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                VisatApp.getApp().clearStatusBarMessage();
            }
        });

//
//        maxField.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                handleMaxTextfield();
//            }
//        });
//
//        maxField.addFocusListener(new FocusListener() {
//            @Override
//            public void focusGained(FocusEvent e) {
//            //    handleMinTextfield();
//            }
//
//            @Override
//            public void focusLost(FocusEvent e) {
//                handleMaxTextfield();
//            }
//        });
//
//
//        minField.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                handleMinTextfield();
//            }
//        });
//
//        minField.addFocusListener(new FocusListener() {
//            @Override
//            public void focusGained(FocusEvent e) {
//            //    handleMaxTextfield();
//            }
//
//            @Override
//            public void focusLost(FocusEvent e) {
//                handleMinTextfield();
//            }
//        });

        maxField.getDocument().addDocumentListener(new DocumentListener() {
            @Override

            public void insertUpdate(DocumentEvent documentEvent) {
                handleMaxTextfield();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        minField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                handleMinTextfield();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });


        bandRange.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VisatApp.getApp().clearStatusBarMessage();
            }
        });


        moreOptionsForm = new MoreOptionsForm(parentForm, true);
        discreteCheckBox = new DiscreteCheckBox(parentForm);
        moreOptionsForm.addRow(discreteCheckBox);


        logDisplayButton = LogDisplay.createButton();
        logDisplayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                boolean originalShouldFireChooserEvent = shouldFireChooserEvent;
//                shouldFireChooserEvent = true;
                if (listenToLogDisplayButtonEnabled[0]) {
                    listenToLogDisplayButtonEnabled[0] = false;
                    logDisplayButton.setSelected(!logDisplayButton.isSelected());
                    applyChanges(RangeKey.ToggleLog);
                    listenToLogDisplayButtonEnabled[0] = true;
                }
//                shouldFireChooserEvent = originalShouldFireChooserEvent;
            }
        });

//        logDisplayButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (listenToLogDisplayButtonEnabled[0]) {
//                    final boolean shouldLog10Display = logDisplayButton.isSelected();
//                    VisatApp.getApp().clearStatusBarMessage();
//                    final ImageInfo imageInfo = parentForm.getImageInfo();
//                    if (shouldLog10Display) {
//                        final ColorPaletteDef cpd = imageInfo.getColorPaletteDef();
//                        if (LogDisplay.checkApplicability(cpd)) {
//                            colorPaletteChooser.setLog10Display(shouldLog10Display);
//                            imageInfo.setLogScaled(shouldLog10Display);
//                            parentForm.applyChanges();
//                        } else {
//                            LogDisplay.showNotApplicableInfo(parentForm.getContentPanel());
//                            logDisplayButton.setSelected(false);
//                        }
//                    } else {
//                        colorPaletteChooser.setLog10Display(shouldLog10Display);
//                        imageInfo.setLogScaled(shouldLog10Display);
//                        parentForm.applyChanges();
//                    }
//                }
//            }
//        });


        colorPaletteSchemes.getStandardJComboBox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleColorPaletteInfoComboBoxSelection(colorPaletteSchemes.getStandardJComboBox());
            }
        });

        colorPaletteSchemes.getUserJComboBox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleColorPaletteInfoComboBoxSelection(colorPaletteSchemes.getUserJComboBox());
            }
        });


//todo  when you lose focus on a combobox and haven't picked something then the font goes white ARGHH!!
        // lose focus listener doesn't appear to help

//        colorPaletteSchemes.getStandardJComboBox().addFocusListener(new FocusListener() {
//            @Override
//            public void focusGained(FocusEvent e) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void focusLost(FocusEvent e) {
//                colorPaletteSchemes.reset();
//            }
//        });
//
//        colorPaletteSchemes.getUserJComboBox().addFocusListener(new FocusListener() {
//            @Override
//            public void focusGained(FocusEvent e) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void focusLost(FocusEvent e) {
//                colorPaletteSchemes.reset();
//            }
//        });
//
//        colorPaletteSchemes.getOtherJCComboBox().addFocusListener(new FocusListener() {
//            @Override
//            public void focusGained(FocusEvent e) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void focusLost(FocusEvent e) {
//                colorPaletteSchemes.reset();
//            }
//        });
//
//        colorPaletteSchemes.getEverythingJComboBox().addFocusListener(new FocusListener() {
//            @Override
//            public void focusGained(FocusEvent e) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void focusLost(FocusEvent e) {
//                colorPaletteSchemes.reset();
//            }
//        });
    }

    private void handleMaxTextfield() {
        if (!maxFieldActivated[0] && !basicSwitcherIsActive[0]) {
            maxFieldActivated[0] = true;
//                    boolean origShouldFireChooserEvent = shouldFireChooserEvent;
//                    shouldFireChooserEvent = true;
            applyChanges(RangeKey.FromMinMaxFields);
//                    shouldFireChooserEvent = origShouldFireChooserEvent;
            maxFieldActivated[0] = false;
        }
    }

    private void handleMinTextfield() {
        if (!minFieldActivated[0] && !basicSwitcherIsActive[0]) {
            minFieldActivated[0] = true;
//                    boolean origShouldFireChooserEvent = shouldFireChooserEvent;
//                    shouldFireChooserEvent = true;
            applyChanges(RangeKey.FromMinMaxFields);
//                    shouldFireChooserEvent = origShouldFireChooserEvent;
            minFieldActivated[0] = false;
        }
    }

    private JPanel getSchemaPanel(String title) {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBorder(BorderFactory.createTitledBorder(title));
        jPanel.setToolTipText("Load a preset color scheme (sets the color-palette, min, max, and log fields)");
        GridBagConstraints gbc = new GridBagConstraints();


        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0,0,3,0);


        jPanel.add(colorScheme, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(0,0,0,0);
        jPanel.add(colorPaletteSchemes.getStandardJComboBox(), gbc);

        gbc.gridy = 2;
        jPanel.add(colorPaletteSchemes.getUserJComboBox(), gbc);

        return jPanel;
    }

    private JPanel getRangePanel(String title) {
        JPanel rangeJPanel = new JPanel(new GridBagLayout());
        rangeJPanel.setBorder(BorderFactory.createTitledBorder(title));
        final GridBagConstraints rangeGbc = new GridBagConstraints();


        minField = getNumberTextField(0);
        maxField = getNumberTextField(1);

        JTextField tmpSizeTextField = new JTextField(("12345678901234567890"));
        minField.setMinimumSize(tmpSizeTextField.getPreferredSize());
        minField.setMaximumSize(tmpSizeTextField.getPreferredSize());
        minField.setPreferredSize(tmpSizeTextField.getPreferredSize());
        maxField.setMinimumSize(tmpSizeTextField.getPreferredSize());
        maxField.setMaximumSize(tmpSizeTextField.getPreferredSize());
        maxField.setPreferredSize(tmpSizeTextField.getPreferredSize());


        JPanel minMaxTextfields = getMinMaxTextfields();


//        final JButton cpdRange = new JButton("Set from CPD File");
//        cpdRange.setToolTipText("Set min and max value to be value in cpd file");
//        cpdRange.setMaximumSize(cpdRange.getMinimumSize());
//        cpdRange.setPreferredSize(cpdRange.getMinimumSize());
//        cpdRange.setMinimumSize(cpdRange.getMinimumSize());


        bandRange = new JButton("Set from Band Data");
        bandRange.setToolTipText("Set min and max value to corresponding data value in the band statistics");
        bandRange.setMaximumSize(bandRange.getMinimumSize());
        bandRange.setPreferredSize(bandRange.getMinimumSize());
        bandRange.setMinimumSize(bandRange.getMinimumSize());


        rangeGbc.fill = GridBagConstraints.HORIZONTAL;
        rangeGbc.anchor = GridBagConstraints.WEST;
        rangeGbc.gridy = 0;
        rangeGbc.gridx = 0;
        rangeGbc.weightx = 1.0;
        rangeJPanel.add(minMaxTextfields, rangeGbc);

        rangeGbc.fill = GridBagConstraints.NONE;
        rangeGbc.gridy = 1;
        rangeGbc.gridx = 0;
        rangeJPanel.add(bandRange, rangeGbc);

//        rangeGbc.fill = GridBagConstraints.NONE;
//        rangeGbc.gridy++;
//        rangeGbc.gridx = 0;
//        rangeJPanel.add(cpdRange, rangeGbc);
        return rangeJPanel;
    }

    private JPanel getColorPaletteFilePanel(String title) {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBorder(BorderFactory.createTitledBorder(title));
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
  //      gbc.insets = new Insets(0,0,3,0);

        jPanel.add(colorPalette, gbc);

   //     gbc.insets = new Insets(0,0,0,0);
        gbc.gridy++;
        jPanel.add(colorPaletteChooser, gbc);

        gbc.gridy++;
        jPanel.add(loadWithCPDFileValuesCheckBox, gbc);



        return jPanel;
    }


    private void handleColorPaletteInfoComboBoxSelection(JComboBox jComboBox) {
        ColorPaletteInfo colorPaletteInfo = (ColorPaletteInfo) jComboBox.getSelectedItem();

        if (colorPaletteInfo.getCpdFilename() != null && colorPaletteInfo.isEnabled()) {

            try {
                if (colorPaletteSchemes.isShouldFire()) {
                    File cpdFile = new File(parentForm.getIODir(), colorPaletteInfo.getCpdFilename());
                    ColorPaletteDef colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(cpdFile);


                    boolean origShouldFireChooserEvent = shouldFireChooserEvent;
                    shouldFireChooserEvent = false;

                    colorPaletteChooser.setSelectedColorPaletteDefinition(colorPaletteDef);
                //    parentForm.getProductSceneView().setColorPaletteInfo(colorPaletteInfo);
                    parentForm.getProductSceneView().getImageInfo().setColorPaletteSchemeName(colorPaletteInfo.getName());

                    applyChanges(colorPaletteInfo.getMinValue(),
                            colorPaletteInfo.getMaxValue(),
                            colorPaletteDef,
                            colorPaletteInfo.isSourceLogScaled(),
                            colorPaletteInfo.isLogScaled(), colorPaletteInfo.getName());



                    shouldFireChooserEvent = origShouldFireChooserEvent;

                    String id = parentForm.getProductSceneView().getRaster().getDisplayName();
                    //   VisatApp.getApp().setStatusBarMessage("Loaded '" + colorPaletteInfo.getName() + "' color schema settings into '" + id);
                    String colorPaletteName = (colorPaletteInfo.getName() != null) ? colorPaletteInfo.getName() : "";
                    VisatApp.getApp().setStatusBarMessage("'" + colorPaletteName + "' color scheme loaded");

                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        colorPaletteSchemes.reset();


    }


    private JLabel spacer1(JLabel lineSpacer) {
        lineSpacer.setText("SPACER");
        Dimension lineSpaceDimension = lineSpacer.getPreferredSize();
        lineSpacer.setPreferredSize(lineSpaceDimension);
        lineSpacer.setMinimumSize(lineSpaceDimension);
        lineSpacer.setText("");
        return lineSpacer;
    }


    private JPanel getMinMaxTextfields() {
        JPanel minMaxTextfields = GridBagUtils.createPanel();
        GridBagConstraints gbc = new GridBagConstraints();


        gbc.anchor = GridBagConstraints.WEST;


        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        minMaxTextfields.add(new JLabel("Min:"), gbc);

        gbc.gridy = 0;
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        minMaxTextfields.add(minField, gbc);


        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        minMaxTextfields.add(new JLabel("Max:"), gbc);
        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        minMaxTextfields.add(maxField, gbc);
        return minMaxTextfields;
    }

    @Override
    public Component getContentPanel() {
        return contentPanel;
    }

    @Override
    public void handleFormShown(ProductSceneView productSceneView) {
        hidden = false;
        updateFormModel(productSceneView);
    }

    @Override
    public void handleFormHidden(ProductSceneView productSceneView) {
        hidden = true;
        if (imageInfoEditor.getModel() != null) {
            imageInfoEditor.setModel(null);
        }
    }

    @Override
    public void updateFormModel(ProductSceneView productSceneView) {
        final ImageInfo imageInfo = productSceneView.getImageInfo();

        if (!hidden) {
            ColorPalettesManager.loadAvailableColorPalettes(parentForm.getIODir());
            colorPaletteChooser.reloadPalettes();
        }

        final ColorPaletteDef cpd = imageInfo.getColorPaletteDef();

        final boolean logScaled = imageInfo.isLogScaled();
        final boolean discrete = cpd.isDiscrete();

        colorPaletteChooser.setLog10Display(logScaled);
        colorPaletteChooser.setDiscreteDisplay(discrete);

        shouldFireChooserEvent = false;
        colorPaletteChooser.setSelectedColorPaletteDefinition(cpd);


        discreteCheckBox.setDiscreteColorsMode(discrete);
        logDisplayButton.setSelected(logScaled);
        parentForm.revalidateToolViewPaneControl();

        if (!minFieldActivated[0]) {
            minField.setValue(cpd.getMinDisplaySample());
        }

        if (!maxFieldActivated[0]) {
            maxField.setValue(cpd.getMaxDisplaySample());
        }

//        ColorPaletteInfo colorPaletteInfo = parentForm.getProductSceneView().getColorPaletteInfo();
//        if (colorPaletteInfo != null && colorPaletteInfo.getName() != null) {
//            colorScheme.setText("Currently Loaded Scheme: " + parentForm.getProductSceneView().getColorPaletteInfo().getName());
//        } else {
//            colorScheme.setText("");
//        }

        String colorPaletteSchemeName = parentForm.getProductSceneView().getImageInfo().getColorPaletteSchemeName();
        if (colorPaletteSchemeName != null && colorPaletteSchemeName.length() > 0) {
            //colorScheme.setText("Currently Loaded Scheme: " + parentForm.getProductSceneView().getColorPaletteSchemeName());
            colorScheme.setText("Current: " + parentForm.getProductSceneView().getImageInfo().getColorPaletteSchemeName());
        } else {
            colorScheme.setText("");
        }

        if (colorPaletteChooser.getSelectedName() != null) {
            colorPalette.setText("Current: " + colorPaletteChooser.getSelectedName());
        }                                                             else {
            colorPalette.setText("");
        }


        shouldFireChooserEvent = true;

    }


    @Override
    public void resetFormModel(ProductSceneView productSceneView) {
        updateFormModel(productSceneView);
        parentForm.revalidateToolViewPaneControl();
    }

    @Override
    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
        if (event.getPropertyName().equals(RasterDataNode.PROPERTY_NAME_STX)) {
            updateFormModel(parentForm.getProductSceneView());
        }
    }

    @Override
    public RasterDataNode[] getRasters() {
        return parentForm.getProductSceneView().getRasters();
    }

    @Override
    public MoreOptionsForm getMoreOptionsForm() {
        return moreOptionsForm;
    }

    @Override
    public AbstractButton[] getToolButtons() {
        return new AbstractButton[]{
                logDisplayButton,
        };
    }

    private ActionListener createListener(final RangeKey key) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyChanges(key);
            }
        };
    }

    private JFormattedTextField getNumberTextField(double value) {
        final NumberFormatter formatter = new NumberFormatter(new DecimalFormat("0.0###########"));
        formatter.setValueClass(Double.class); // to ensure that double values are returned
        final JFormattedTextField numberField = new JFormattedTextField(formatter);
        numberField.setValue(value);
        final Dimension preferredSize = numberField.getPreferredSize();
        preferredSize.width = 70;
        numberField.setPreferredSize(preferredSize);
        return numberField;
    }

    private void applyChanges(RangeKey key) {
        if (shouldFireChooserEvent) {

            
            final ColorPaletteDef selectedCPD = colorPaletteChooser.getSelectedColorPaletteDefinition();
            final ImageInfo currentInfo = parentForm.getImageInfo();
            final ColorPaletteDef currentCPD = currentInfo.getColorPaletteDef();
            final ColorPaletteDef deepCopy = selectedCPD.createDeepCopy();
            deepCopy.setDiscrete(currentCPD.isDiscrete());

            final double min;
            final double max;
            final boolean isSourceLogScaled;
            final boolean isTargetLogScaled;
            final ColorPaletteDef cpd;
            final boolean autoDistribute;

            switch (key) {
                case FromPaletteSource:
                    modifyColorPaletteSchemeName();
                    final Range rangeFromFile = colorPaletteChooser.getRangeFromFile();
                    isSourceLogScaled = currentInfo.isLogScaled();
                    isTargetLogScaled = currentInfo.isLogScaled();
                    min = rangeFromFile.getMin();
                    max = rangeFromFile.getMax();
                    cpd = currentCPD;
                    autoDistribute = true;
                    break;
                case FromData:
                    modifyColorPaletteSchemeName();
                    final Stx stx = parentForm.getStx(parentForm.getProductSceneView().getRaster());
                    isSourceLogScaled = currentInfo.isLogScaled();
                    isTargetLogScaled = currentInfo.isLogScaled();
                    min = stx.getMinimum();
                    max = stx.getMaximum();
                    cpd = currentCPD;
                    autoDistribute = true;
                    break;
                case FromMinMaxFields:
                    modifyColorPaletteSchemeName();
                    isSourceLogScaled = currentInfo.isLogScaled();
                    isTargetLogScaled = currentInfo.isLogScaled();
                    min = new Double(minField.getValue().toString());//(double) minField.getValue();
                    max = new Double(maxField.getValue().toString());//(double) maxField.getValue();
                    cpd = currentCPD;
                    autoDistribute = true;
                    break;
                case ToggleLog:
                    modifyColorPaletteSchemeName();
                    isSourceLogScaled = currentInfo.isLogScaled();
                    isTargetLogScaled = !currentInfo.isLogScaled();
                    min = currentCPD.getMinDisplaySample();
                    max = currentCPD.getMaxDisplaySample();
                    cpd = currentCPD;
                    autoDistribute = true;
                    break;
                default:
                    currentInfo.setColorPaletteSchemeName(null);
                    if (loadWithCPDFileValuesCheckBox.isSelected()) {
                        isSourceLogScaled = selectedCPD.isLogScaled();
                        isTargetLogScaled = selectedCPD.isLogScaled();
                        //   autoDistribute = selectedCPD.isAutoDistribute();
                        autoDistribute = false;
                        currentInfo.setLogScaled(isTargetLogScaled);
                        min = selectedCPD.getMinDisplaySample();
                        max = selectedCPD.getMaxDisplaySample();
                        cpd = deepCopy;
                        deepCopy.setLogScaled(isTargetLogScaled);
                        deepCopy.setAutoDistribute(autoDistribute);
                        if (testMinMax(min, max, isTargetLogScaled)) {
                            listenToLogDisplayButtonEnabled[0] = false;
                            logDisplayButton.setSelected(isTargetLogScaled);
                            listenToLogDisplayButtonEnabled[0] = true;
                        }
                    } else {
                        isSourceLogScaled = false;
                        isTargetLogScaled = currentInfo.isLogScaled();
                        min = currentCPD.getMinDisplaySample();
                        max = currentCPD.getMaxDisplaySample();
                        cpd = deepCopy;
                        autoDistribute = true;
                    }


            }


            if (testMinMax(min, max, isTargetLogScaled)) {
                currentInfo.setColorPaletteDef(cpd, min, max, autoDistribute, isSourceLogScaled, isTargetLogScaled);

                if (key == RangeKey.ToggleLog) {
                    currentInfo.setLogScaled(isTargetLogScaled);
                    colorPaletteChooser.setLog10Display(isTargetLogScaled);
                }
                parentForm.applyChanges();


            }

        }
    }


    private void modifyColorPaletteSchemeName() {
        if (parentForm.getImageInfo().getColorPaletteSchemeName() != null && !parentForm.getImageInfo().getColorPaletteSchemeName().endsWith("*") && parentForm.getImageInfo().getColorPaletteSchemeName().length() > 0) {
            parentForm.getImageInfo().setColorPaletteSchemeName(parentForm.getImageInfo().getColorPaletteSchemeName()+"*");
        }
    }


    private boolean testMinMax(double min, double max, boolean isLogScaled) {
        boolean checksOut = true;

        if (min == max) {
            checksOut = false;
            VisatApp.getApp().setStatusBarMessage("WARNING: Min cannot equal Max");
//            JOptionPane.showMessageDialog(minField, "Min cannot equal Max");
        }

        if (isLogScaled && min == 0) {
            checksOut = false;
            VisatApp.getApp().setStatusBarMessage("WARNING: Min cannot be 0 in log scaling mode");
//            JOptionPane.showMessageDialog(minField, "Min cannot be 0 in log scaling mode");
        }

        if (checksOut) {
            VisatApp.getApp().clearStatusBarMessage();
        }
        return checksOut;
    }

    private void applyChanges(double min, double max, ColorPaletteDef selectedCPD, boolean isSourceLogScaled, boolean isTargetLogScaled, String colorSchemaName) {
        final ImageInfo currentInfo = parentForm.getImageInfo();
        final ColorPaletteDef currentCPD = currentInfo.getColorPaletteDef();
        final ColorPaletteDef deepCopy = selectedCPD.createDeepCopy();
        deepCopy.setDiscrete(currentCPD.isDiscrete());
        deepCopy.setAutoDistribute(true);

        final boolean autoDistribute = true;
        currentInfo.setLogScaled(isTargetLogScaled);
        currentInfo.setColorPaletteDef(selectedCPD, min, max, autoDistribute, isSourceLogScaled, isTargetLogScaled);
        currentInfo.setColorPaletteSchemeName(colorSchemaName);
        parentForm.applyChanges();
    }


}