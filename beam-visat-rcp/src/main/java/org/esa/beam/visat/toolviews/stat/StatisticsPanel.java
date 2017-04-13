/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.datamodel.StxFactory;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.TextFieldContainer;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XIntervalSeries;
import org.jfree.data.xy.XIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.media.jai.Histogram;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * A general pane within the statistics window.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Daniel Knowles
 */
class StatisticsPanel extends PagePanel implements MultipleRoiComputePanel.ComputeMasks, StatisticsDataProvider {

    final VisatApp visatApp = VisatApp.getApp();
    private PropertyMap configuration = null;

    ProgressMonitorSwingWorker swingWorker;

    private static final String DEFAULT_STATISTICS_TEXT = "No statistics computed";  /*I18N*/
    private static final String TITLE_PREFIX = "Statistics";

    private static final int NUM_BINS_INVALID = -999;
    private static final int DECIMAL_PLACES_DEFAULT = 4;
    private static final int DECIMAL_PLACES_FULL = -1;


    private static final int COL_WIDTH_DEFAULT = 10;
    private static final String TEXTFIELD_NAME_COL_WIDTH = "Column Width (Spreadsheet)";


    private int colCharWidth = COL_WIDTH_DEFAULT;


    private boolean includeMedian = true;

    private double spreadsheetHeightWeight = 0.3;
    private int spreadsheetMinRowsBeforeWeight = 10;

    boolean invertPercentile = true;


    // todo Danny
// this works with the exception that statistics returns these values as the Minimum and Maximum.

    boolean binRange = false;   // use this to set binMin and binMax to Double.NaN
    double binMin = Double.NaN;
    double binMax = Double.NaN;
    double binWidth = Double.NaN;


    boolean fixedHistDomainAllPlots = false;
    boolean fixedHistDomainAllPlotsInitialized = false;
    double[] histDomainBoundsAllPlots = {0, 0};

    boolean fixedPercentileDomainAllPlots = true;
    boolean fixedPercentileDomainAllPlotsInitialized = false;
    double[] histRangeBoundsAllPlots = {0, 0};
    double[] percentileDomainBoundsAllPlots = {0, 0};
    double[] percentileRangeBoundsAllPlots = {0, 0};


    private MultipleRoiComputePanel computePanel;
    private JPanel backgroundPanel;
    private AbstractButton hideAndShowButton;
    private AbstractButton exportButton;
    private JPanel contentPanel;
    private JPanel accuracyPanel;
    private JPanel spreadsheetPanel;
    JScrollPane spreadsheetScrollPane;
    JScrollPane contentScrollPane;
    JPanel leftPanel;

    private final StatisticsPanel.PopupHandler popupHandler;
    private final StringBuilder resultText;

    private boolean init;
    private Histogram[] histograms;
    private ExportStatisticsAsCsvAction exportAsCsvAction;
    private PutStatisticsIntoVectorDataAction putStatisticsIntoVectorDataAction;
    //   private AccuracyModel accuracyModel;
    private boolean LogMode = false;  // keeping false for the moment as all the nuances of log mode may not be worked out
    private int numBins = StatisticsToolView.PARAM_DEFVAL_NUM_BINS;
    private String percentThresholds = StatisticsToolView.PARAM_DEFVAL_PERCENT_THRESHOLDS;
    private List<Integer> percentThresholdsList = null;


    private boolean exportButtonEnabled = false;
    private boolean exportButtonVisible = false;

    private boolean handlersEnabled = true;


    private boolean showPercentPlots = StatisticsToolView.PARAM_DEFVAL_PERCENT_PLOT_ENABLED;
    private boolean showHistogramPlots = StatisticsToolView.PARAM_DEFVAL_HISTOGRAM_PLOT_ENABLED;
    private boolean showStatsList = StatisticsToolView.PARAM_DEFVAL_STATS_LIST_ENABLED;
    private boolean showStatsSpreadSheet = StatisticsToolView.PARAM_DEFVAL_STATS_SPREADSHEET_ENABLED;


    private boolean includeHistogramStats = false;
    private int decimalPlaces = DECIMAL_PLACES_DEFAULT;
    private Object[][] statsSpreadsheet;
    private int currRow = 1;

    private int numStatisticFields = 0;
    private int numProductRegions = 0;


    private boolean plotsThreshDomainSpan = StatisticsToolView.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_SPAN;
    private double plotsThreshDomainLow = StatisticsToolView.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_LOW;
    private double plotsThreshDomainHigh = StatisticsToolView.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_HIGH;
    private JCheckBox plotsThreshDomainSpanCheckBox = null;
    private TextFieldContainer plotsThreshDomainLowTextfieldContainer = null;
    private TextFieldContainer plotsThreshDomainHighTextfieldContainer = null;


    private boolean plotsDomainSpan = StatisticsToolView.PARAM_DEFVAL_PLOTS_DOMAIN_SPAN;
    private double plotsDomainLow = StatisticsToolView.PARAM_DEFVAL_PLOTS_DOMAIN_LOW;
    private double plotsDomainHigh = StatisticsToolView.PARAM_DEFVAL_PLOTS_DOMAIN_HIGH;
    private JCheckBox plotsDomainSpanCheckBox = null;
    private TextFieldContainer plotsDomainLowTextfieldContainer = null;
    private TextFieldContainer plotsDomainHighTextfieldContainer = null;


    private int plotMinHeight = 300;
    private int plotMinWidth = 300;

    private boolean exactPlotSize = StatisticsToolView.PARAM_DEFVAL_PLOTS_SIZE;
    private int plotSizeHeight = StatisticsToolView.PARAM_DEFVAL_PLOTS_SIZE_HEIGHT;
    private int plotSizeWidth = StatisticsToolView.PARAM_DEFVAL_PLOTS_SIZE_WIDTH;
    private JCheckBox plotsSizeCheckBox = null;
    private TextFieldContainer plotsSizeHeightTextfieldContainer = null;
    private TextFieldContainer plotsSizeWidthTextfieldContainer = null;


    private TextFieldContainer numBinsTextfieldContainer = null;
    private TextFieldContainer spreadsheetColWidthTextfieldContainer = null;


    //   private HistDisplayHighThreshTextfield plotsThreshDomainHighTextfieldContainer = new HistDisplayHighThreshTextfield();


    public StatisticsPanel(final ToolView parentDialog, String helpID) {
        super(parentDialog, helpID, TITLE_PREFIX);
        setMinimumSize(new Dimension(1000, 390));
        resultText = new StringBuilder();
        popupHandler = new PopupHandler();
        if (visatApp != null) {
            this.configuration = visatApp.getPreferences();
        }
    }

    @Override
    protected void initComponents() {
        init = true;

        statsSpreadsheet = null;

        showHistogramPlots = getPreferencesHistogramPlotEnabled();
        showPercentPlots = getPreferencesPercentPlotEnabled();
        showStatsList = getPreferencesStatsListEnabled();
        showStatsSpreadSheet = getPreferencesStatsSpreadSheetEnabled();
        numBins = getPreferencesNumBins();
        percentThresholds = getPreferencesPercentThresholds();


        plotsThreshDomainLowTextfieldContainer = new TextFieldContainer(StatisticsToolView.PARAM_SHORTLABEL_PLOTS_THRESH_DOMAIN_LOW,
                StatisticsToolView.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_LOW,
                StatisticsToolView.PARAM_MINVAL_PLOTS_THRESH_DOMAIN_LOW,
                StatisticsToolView.PARAM_MAXVAL_PLOTS_THRESH_DOMAIN_LOW,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane());
        plotsThreshDomainLowTextfieldContainer.setEnabled(StatisticsToolView.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_SPAN);


        plotsThreshDomainHighTextfieldContainer = new TextFieldContainer(StatisticsToolView.PARAM_SHORTLABEL_PLOTS_THRESH_DOMAIN_HIGH,
                StatisticsToolView.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_HIGH,
                StatisticsToolView.PARAM_MINVAL_PLOTS_THRESH_DOMAIN_HIGH,
                StatisticsToolView.PARAM_MAXVAL_PLOTS_THRESH_DOMAIN_HIGH,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane());
        plotsThreshDomainHighTextfieldContainer.setEnabled(StatisticsToolView.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_SPAN);


        plotsDomainLowTextfieldContainer = new TextFieldContainer(StatisticsToolView.PARAM_LABEL_PLOTS_DOMAIN_LOW,
                StatisticsToolView.PARAM_DEFVAL_PLOTS_DOMAIN_LOW,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane());
        plotsDomainLowTextfieldContainer.setEnabled(StatisticsToolView.PARAM_DEFVAL_PLOTS_DOMAIN_SPAN);


        plotsDomainHighTextfieldContainer = new TextFieldContainer(StatisticsToolView.PARAM_LABEL_PLOTS_DOMAIN_HIGH,
                StatisticsToolView.PARAM_DEFVAL_PLOTS_DOMAIN_HIGH,
                TextFieldContainer.NumType.DOUBLE,
                4,
                getParentDialogContentPane());
        plotsDomainHighTextfieldContainer.setEnabled(StatisticsToolView.PARAM_DEFVAL_PLOTS_DOMAIN_SPAN);


        plotsSizeHeightTextfieldContainer = new TextFieldContainer(StatisticsToolView.PARAM_SHORTLABEL_PLOTS_SIZE_HEIGHT,
                StatisticsToolView.PARAM_DEFVAL_PLOTS_SIZE_HEIGHT,
                StatisticsToolView.PARAM_MINVAL_PLOTS_SIZE_HEIGHT,
                StatisticsToolView.PARAM_MAXVAL_PLOTS_SIZE_HEIGHT,
                TextFieldContainer.NumType.INT,
                4,
                getParentDialogContentPane());
        plotsSizeHeightTextfieldContainer.setEnabled(StatisticsToolView.PARAM_DEFVAL_PLOTS_SIZE);


        plotsSizeWidthTextfieldContainer = new TextFieldContainer(StatisticsToolView.PARAM_SHORTLABEL_PLOTS_SIZE_WIDTH,
                StatisticsToolView.PARAM_DEFVAL_PLOTS_SIZE_WIDTH,
                StatisticsToolView.PARAM_MINVAL_PLOTS_SIZE_WIDTH,
                StatisticsToolView.PARAM_MAXVAL_PLOTS_SIZE_WIDTH,
                TextFieldContainer.NumType.INT,
                4,
                getParentDialogContentPane());
        plotsSizeWidthTextfieldContainer.setEnabled(StatisticsToolView.PARAM_DEFVAL_PLOTS_SIZE);


        plotsThreshDomainSpanCheckBox = new JCheckBox(StatisticsToolView.PARAM_SHORTLABEL_PLOTS_THRESH_DOMAIN_SPAN);
        plotsThreshDomainSpanCheckBox.setSelected(StatisticsToolView.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_SPAN);


        plotsDomainSpanCheckBox = new JCheckBox(StatisticsToolView.PARAM_SHORTLABEL_PLOTS_DOMAIN_SPAN);
        plotsDomainSpanCheckBox.setSelected(StatisticsToolView.PARAM_DEFVAL_PLOTS_DOMAIN_SPAN);

        plotsSizeCheckBox = new JCheckBox(StatisticsToolView.PARAM_SHORTLABEL_PLOTS_SIZE);
        plotsSizeCheckBox.setSelected(StatisticsToolView.PARAM_DEFVAL_PLOTS_SIZE);


        plotsSizeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                plotsSizeHeightTextfieldContainer.setEnabled(plotsSizeCheckBox.isSelected());
                plotsSizeWidthTextfieldContainer.setEnabled(plotsSizeCheckBox.isSelected());
            }
        });


        plotsThreshDomainSpanCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                plotsThreshDomainHighTextfieldContainer.setEnabled(plotsThreshDomainSpanCheckBox.isSelected());
                plotsThreshDomainLowTextfieldContainer.setEnabled(plotsThreshDomainSpanCheckBox.isSelected());

                if (plotsThreshDomainSpanCheckBox.isSelected()) {
                    plotsDomainSpanCheckBox.setSelected(false);
                    plotsDomainHighTextfieldContainer.setEnabled(false);
                    plotsDomainLowTextfieldContainer.setEnabled(false);
                }
            }
        });

        plotsDomainSpanCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                plotsDomainHighTextfieldContainer.setEnabled(plotsDomainSpanCheckBox.isSelected());
                plotsDomainLowTextfieldContainer.setEnabled(plotsDomainSpanCheckBox.isSelected());

                if (plotsDomainSpanCheckBox.isSelected()) {
                    plotsThreshDomainSpanCheckBox.setSelected(false);
                    plotsThreshDomainHighTextfieldContainer.setEnabled(false);
                    plotsThreshDomainLowTextfieldContainer.setEnabled(false);
                }
            }
        });


        numBinsTextfieldContainer = new TextFieldContainer(StatisticsToolView.PARAM_LABEL_NUM_BINS,
                numBins,
                StatisticsToolView.PARAM_MINVAL_NUM_BINS,
                StatisticsToolView.PARAM_MAXVAL_NUM_BINS,
                TextFieldContainer.NumType.INT,
                7,
                getParentDialogContentPane());


        spreadsheetColWidthTextfieldContainer = new TextFieldContainer(StatisticsToolView.PARAM_LABEL_SPREADSHEET_COL_WIDTH,
                StatisticsToolView.PARAM_DEFVAL_SPREADSHEET_COL_WIDTH,
                StatisticsToolView.PARAM_MINVAL_SPREADSHEET_COL_WIDTH,
                StatisticsToolView.PARAM_MAXVAL_SPREADSHEET_COL_WIDTH,
                TextFieldContainer.NumType.INT,
                2,
                getParentDialogContentPane());


        final JPanel rightPanel = getRightPanel();

        final ImageIcon collapseIcon = UIUtils.loadImageIcon("icons/PanelRight12.png");
        final ImageIcon collapseRolloverIcon = ToolButtonFactory.createRolloverIcon(collapseIcon);
        final ImageIcon expandIcon = UIUtils.loadImageIcon("icons/PanelLeft12.png");
        final ImageIcon expandRolloverIcon = ToolButtonFactory.createRolloverIcon(expandIcon);

        hideAndShowButton = ToolButtonFactory.createButton(collapseIcon, false);
        hideAndShowButton.setToolTipText("Collapse Options Panel");
        hideAndShowButton.setName("switchToChartButton");
        hideAndShowButton.addActionListener(new ActionListener() {

            public boolean rightPanelShown;

            @Override
            public void actionPerformed(ActionEvent e) {
                rightPanel.setVisible(rightPanelShown);
                if (rightPanelShown) {
                    hideAndShowButton.setIcon(collapseIcon);
                    hideAndShowButton.setRolloverIcon(collapseRolloverIcon);
                    hideAndShowButton.setVisible(true);
                    hideAndShowButton.setToolTipText("Collapse Options Panel");
                } else {
                    hideAndShowButton.setIcon(expandIcon);
                    hideAndShowButton.setRolloverIcon(expandRolloverIcon);
                    hideAndShowButton.setVisible(true);
                    hideAndShowButton.setToolTipText("Expand Options Panel");
                }
                rightPanelShown = !rightPanelShown;
            }
        });

        hideAndShowButton.setVisible(true);


        contentPanel = new JPanel(new GridLayout(-1, 1));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.addMouseListener(popupHandler);

        contentScrollPane = new JScrollPane(contentPanel);
        contentScrollPane.setBorder(null);
        contentScrollPane.setBackground(Color.WHITE);


        spreadsheetPanel = new JPanel(new GridLayout(-1, 1));
        //    spreadsheetPanel.setBackground(Color.WHITE);
        spreadsheetPanel.addMouseListener(popupHandler);

        spreadsheetScrollPane = new JScrollPane(spreadsheetPanel);


        spreadsheetScrollPane.setBorder(null);
        //    spreadsheetScrollPane.setBackground(Color.WHITE);
        spreadsheetScrollPane.setMinimumSize(new Dimension(100, 100));
        spreadsheetScrollPane.setBorder(UIUtils.createGroupBorder("Statistics Spreadsheet"));
        spreadsheetScrollPane.setVisible(showStatsSpreadSheet);


        leftPanel = new JPanel(new GridBagLayout());
//        GridBagConstraints gbcLeftPanel = new GridBagConstraints();
//        GridBagUtils.addToPanel(leftPanel, contentScrollPane, gbcLeftPanel, "fill=BOTH, weightx=1.0, weighty=1.0, anchor=NORTHWEST");
//        GridBagUtils.addToPanel(leftPanel, spreadsheetScrollPane, gbcLeftPanel, "fill=BOTH, weightx=1.0, weighty=0.0, anchor=NORTHWEST, gridy=1,insets.top=5");

        initLeftPanel();

        backgroundPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagUtils.addToPanel(backgroundPanel, leftPanel, gbc, "fill=BOTH, weightx=1.0, weighty=1.0, anchor=NORTHWEST,insets.right=5");
        //   GridBagUtils.addToPanel(backgroundPanel, contentScrollPane, gbc, "fill=BOTH, weightx=1.0, weighty=1.0, anchor=NORTHWEST");
        //  GridBagUtils.addToPanel(backgroundPanel, spreadsheetScrollPane, gbc, "fill=BOTH, weightx=1.0, weighty=1.0, anchor=NORTHWEST, gridy=1");
        GridBagUtils.addToPanel(backgroundPanel, rightPanel, gbc, "gridx=1,gridy=0, fill=BOTH, weightx=0.0,anchor=NORTHEAST,insets.left=5");


        //   GridBagUtils.addToPanel(backgroundPanel, spreadsheetScrollPane, gbcLeftPanel, "fill=HORIZONTAL, weightx=1.0, weighty=1.0, anchor=NORTHWEST, gridy=1,gridx=0,gridwidth=2,insets.top=5");
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.add(backgroundPanel, new Integer(0));
        layeredPane.add(hideAndShowButton, new Integer(1));
        add(layeredPane);


        int minWidth = leftPanel.getMinimumSize().width + rightPanel.getMinimumSize().width;
        int minHeight = Math.max(leftPanel.getMinimumSize().height, rightPanel.getMinimumSize().height);
        setMinimumSize(new Dimension(minWidth, minHeight));
    }


    private JPanel getThresholdsPanel() {

        final JLabel label = new JLabel(StatisticsToolView.PARAM_LABEL_PERCENT_THRESHOLDS);
        final JTextField textField = new JTextField(14);
        textField.setName(StatisticsToolView.PARAM_LABEL_PERCENT_THRESHOLDS);
        textField.setText(percentThresholds);
        textfieldHandler(textField);


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 3;
        panel.add(label, gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        panel.add(textField, gbc);

        return panel;
    }


    private JPanel getColWidthPanel() {


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 3;
        panel.add(spreadsheetColWidthTextfieldContainer.getLabel(), gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        panel.add(spreadsheetColWidthTextfieldContainer.getTextfield(), gbc);

        return panel;
    }


    private JPanel getPlotsThreshDomainSpanPanel() {


        JPanel childPanel = GridBagUtils.createPanel();
        GridBagConstraints gbcChild = GridBagUtils.createConstraints();

        gbcChild.gridx += 1;
        gbcChild.insets.left = new JCheckBox(" ").getPreferredSize().width;
        gbcChild.insets.right = 3;
        childPanel.add(plotsThreshDomainLowTextfieldContainer.getLabel(), gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        childPanel.add(plotsThreshDomainLowTextfieldContainer.getTextfield(), gbcChild);

        gbcChild.gridx += 1;
        gbcChild.insets.right = 3;
        childPanel.add(plotsThreshDomainHighTextfieldContainer.getLabel(), gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        childPanel.add(plotsThreshDomainHighTextfieldContainer.getTextfield(), gbcChild);


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        panel.add(plotsThreshDomainSpanCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(childPanel, gbc);


        return panel;
    }


    private JPanel getPlotsDomainSpanPanel() {


        JPanel childPanel = GridBagUtils.createPanel();
        GridBagConstraints gbcChild = GridBagUtils.createConstraints();

        gbcChild.gridx += 1;
        gbcChild.insets.left = new JCheckBox(" ").getPreferredSize().width;
        gbcChild.insets.right = 3;
        childPanel.add(plotsDomainLowTextfieldContainer.getLabel(), gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        childPanel.add(plotsDomainLowTextfieldContainer.getTextfield(), gbcChild);

        gbcChild.gridx += 1;
        gbcChild.insets.right = 3;
        childPanel.add(plotsDomainHighTextfieldContainer.getLabel(), gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        childPanel.add(plotsDomainHighTextfieldContainer.getTextfield(), gbcChild);


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        panel.add(plotsDomainSpanCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(childPanel, gbc);


        return panel;
    }


    private JPanel getPlotsSizePanel() {


        JPanel childPanel = GridBagUtils.createPanel();
        GridBagConstraints gbcChild = GridBagUtils.createConstraints();

        gbcChild.gridx += 1;
        gbcChild.insets.left = new JCheckBox(" ").getPreferredSize().width;
        gbcChild.insets.right = 3;
        childPanel.add(plotsSizeWidthTextfieldContainer.getLabel(), gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        childPanel.add(plotsSizeWidthTextfieldContainer.getTextfield(), gbcChild);

        gbcChild.gridx += 1;
        gbcChild.insets.right = 3;
        childPanel.add(plotsSizeHeightTextfieldContainer.getLabel(), gbcChild);
        gbcChild = GridBagUtils.restoreConstraints(gbcChild);

        gbcChild.gridx += 1;
        childPanel.add(plotsSizeHeightTextfieldContainer.getTextfield(), gbcChild);


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        panel.add(plotsSizeCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(childPanel, gbc);


        return panel;
    }


    private JPanel getNumBinsPanel() {


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 3;
        panel.add(numBinsTextfieldContainer.getLabel(), gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        panel.add(numBinsTextfieldContainer.getTextfield(), gbc);

        return panel;
    }


    private JPanel getDecimalPlacesPanel() {

        final JLabel label = new JLabel("Decimal Places");
        String[] decimalPlacesList = {"0", "1", "2", "3", "4", "5", "6", " FULL"};
        SpinnerListModel decimalPlacesSpinnerModel = new SpinnerListModel(decimalPlacesList);
        final JSpinner decimalPlacesSpinner = new JSpinner(decimalPlacesSpinnerModel);
        decimalPlacesSpinner.setValue(" FULL");
        decimalPlacesSpinner.setMinimumSize(decimalPlacesSpinner.getPreferredSize());
        decimalPlacesSpinner.setPreferredSize(decimalPlacesSpinner.getPreferredSize());
        decimalPlacesSpinner.setValue("4");

        decimalPlacesSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                String decimalPlacesString = decimalPlacesSpinner.getValue().toString();

                if (" FULL".equals(decimalPlacesString)) {
                    decimalPlaces = DECIMAL_PLACES_FULL;
                } else {
                    decimalPlaces = Integer.parseInt(decimalPlacesString);
                }
            }
        });


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 3;
        panel.add(label, gbc);
        gbc.insets.right = 0;
        gbc.gridx++;
        panel.add(decimalPlacesSpinner, gbc);

        return panel;
    }


    private JPanel getDisplayOptionsPanel() {

        JPanel thresholdsPanel = getThresholdsPanel();
        JPanel colWidthPanel = getColWidthPanel();
        JPanel decimalPlacesPanel = getDecimalPlacesPanel();


        final JCheckBox histogramStatsCheckBox = new JCheckBox("Show Histogram Statistics");
        histogramStatsCheckBox.setSelected(includeHistogramStats);
        histogramStatsCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeHistogramStats = histogramStatsCheckBox.isSelected();

            }
        });


//        final JCheckBox showPercentPlotCheckBox = new JCheckBox(StatisticsToolView.PARAM_LABEL_PERCENT_PLOT_ENABLED);
//        showPercentPlotCheckBox.setSelected(showPercentPlots);
//        showPercentPlotCheckBox.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                showPercentPlots = showPercentPlotCheckBox.isSelected();
//
//            }
//        });
//
//
//        final JCheckBox showHistogramPlotCheckBox = new JCheckBox(StatisticsToolView.PARAM_LABEL_HISTOGRAM_PLOT_ENABLED);
//        showHistogramPlotCheckBox.setSelected(showHistogramPlots);
//        showHistogramPlotCheckBox.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                showHistogramPlots = showHistogramPlotCheckBox.isSelected();
//
//            }
//        });


        final JCheckBox includeMedianCheckBox = new JCheckBox("Show Median");
        includeMedianCheckBox.setSelected(includeMedian);
        includeMedianCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeMedian = includeMedianCheckBox.isSelected();

            }
        });


        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets.top = 5;
        gbc.anchor = GridBagConstraints.NORTH;
//        panel.add(new TitledSeparator("Display Options", SwingConstants.CENTER), gbc);
//        gbc = GridBagUtils.restoreConstraints(gbc);
//        gbc.gridy += 1;


//        panel.add(includeMedianCheckBox, gbc);
//        gbc.gridy += 1;
//
//        panel.add(histogramStatsCheckBox, gbc);
//        gbc.gridy += 1;
//
//        panel.add(thresholdsPanel, gbc);
//        gbc.gridy += 1;

        panel.add(decimalPlacesPanel, gbc);
        gbc.gridy += 1;
        gbc.insets.top = 0;


        panel.add(colWidthPanel, gbc);


        JPanel junk = new JPanel();
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(junk, gbc);

        return panel;
    }

    private JPanel getFieldOptionsPanel() {

        JPanel thresholdsPanel = getThresholdsPanel();


        final JCheckBox histogramStatsCheckBox = new JCheckBox("Show Histogram Statistics");
        histogramStatsCheckBox.setSelected(includeHistogramStats);
        histogramStatsCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeHistogramStats = histogramStatsCheckBox.isSelected();

            }
        });


        final JCheckBox includeMedianCheckBox = new JCheckBox("Show Median");
        includeMedianCheckBox.setSelected(includeMedian);
        includeMedianCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                includeMedian = includeMedianCheckBox.isSelected();

            }
        });


        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets.top = 5;
        //    panel.add(new TitledSeparator("Display Options", SwingConstants.CENTER), gbc);
        //       gbc = GridBagUtils.restoreConstraints(gbc);
        //  gbc.gridy += 1;


        panel.add(includeMedianCheckBox, gbc);
        gbc.insets.top = 0;
        gbc.gridy += 1;

        panel.add(histogramStatsCheckBox, gbc);
        gbc.gridy += 1;

        panel.add(thresholdsPanel, gbc);


        JPanel junk = new JPanel();
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(junk, gbc);

        return panel;
    }


    private JPanel getPlotsOptionsPanel() {
        final JCheckBox showStatsListCheckBox = new JCheckBox(StatisticsToolView.PARAM_LABEL_STATS_LIST_ENABLED);
        showStatsListCheckBox.setSelected(showStatsList);
        showStatsListCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                showStatsList = showStatsListCheckBox.isSelected();

            }
        });


        final JCheckBox showStatsSpreadSheetCheckBox = new JCheckBox(StatisticsToolView.PARAM_LABEL_STATS_SPREADSHEET_ENABLED);
        showStatsSpreadSheetCheckBox.setSelected(showStatsSpreadSheet);
        showStatsSpreadSheetCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                showStatsSpreadSheet = showStatsSpreadSheetCheckBox.isSelected();

            }
        });
        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

        gbc.weighty = 0;
        gbc.insets.top = 5;
        gbc.insets.bottom = 3;
        panel.add(getPlotsThreshDomainSpanPanel(), gbc);
        gbc = GridBagUtils.restoreConstraints(gbc);

        gbc.gridy += 1;
        gbc.insets.bottom = 3;
        panel.add(getPlotsDomainSpanPanel(), gbc);
        gbc = GridBagUtils.restoreConstraints(gbc);

        // todo PlotSize
        gbc.gridy += 1;
        gbc.insets.bottom = 3;
        panel.add(getPlotsSizePanel(), gbc);
        gbc = GridBagUtils.restoreConstraints(gbc);


        gbc.gridy += 1;
        JPanel junk = new JPanel();
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(junk, gbc);

        return panel;
    }

    private JPanel getViewPanel() {


        final JCheckBox showStatsListCheckBox = new JCheckBox(StatisticsToolView.PARAM_LABEL_STATS_LIST_ENABLED);
        showStatsListCheckBox.setSelected(showStatsList);
        showStatsListCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                showStatsList = showStatsListCheckBox.isSelected();

            }
        });


        final JCheckBox showStatsSpreadSheetCheckBox = new JCheckBox(StatisticsToolView.PARAM_LABEL_STATS_SPREADSHEET_ENABLED);
        showStatsSpreadSheetCheckBox.setSelected(showStatsSpreadSheet);
        showStatsSpreadSheetCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                showStatsSpreadSheet = showStatsSpreadSheetCheckBox.isSelected();

            }
        });


        final JCheckBox showPercentPlotCheckBox = new JCheckBox(StatisticsToolView.PARAM_LABEL_PERCENT_PLOT_ENABLED);
        showPercentPlotCheckBox.setSelected(showPercentPlots);
        showPercentPlotCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                showPercentPlots = showPercentPlotCheckBox.isSelected();

            }
        });


        final JCheckBox showHistogramPlotCheckBox = new JCheckBox(StatisticsToolView.PARAM_LABEL_HISTOGRAM_PLOT_ENABLED);
        showHistogramPlotCheckBox.setSelected(showHistogramPlots);
        showHistogramPlotCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                showHistogramPlots = showHistogramPlotCheckBox.isSelected();

            }
        });


        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();


        gbc.weighty = 0;
        gbc.insets.top = 5;
        panel.add(showPercentPlotCheckBox, gbc);
        gbc.insets.top = 0;

        gbc.gridy += 1;
        // gbc.insets.top = 3;
        panel.add(showHistogramPlotCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(showStatsListCheckBox, gbc);

        gbc.gridy += 1;
        panel.add(showStatsSpreadSheetCheckBox, gbc);

        gbc.gridy += 1;
        JPanel junk = new JPanel();
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(junk, gbc);

        return panel;
    }


    private JPanel getBinningCriteriaPanel() {

        JPanel numBinsPanel = getNumBinsPanel();

        final JCheckBox logModeCheckBox = new JCheckBox("Log Scaled Bins");
        logModeCheckBox.setSelected(LogMode);
        logModeCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                LogMode = logModeCheckBox.isSelected();

            }
        });


        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();

//        gbc.fill = GridBagConstraints.HORIZONTAL;
//        gbc.weightx = 1;
//        gbc.weighty = 0;
//        gbc.anchor = GridBagConstraints.NORTH;
//        gbc.insets.top = 5;
        //    panel.add(new TitledSeparator("Binning Criteria", SwingConstants.CENTER), gbc);
        //    gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.insets.top = 5;
        //   gbc.gridy += 1;
        gbc.weighty = 0;
        panel.add(numBinsPanel, gbc);

        gbc.gridy += 1;
        gbc.insets.top = 0;
        panel.add(logModeCheckBox, gbc);

        JPanel junk = new JPanel();
        gbc = GridBagUtils.restoreConstraints(gbc);
        gbc.weighty = 1;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(junk, gbc);

        return panel;
    }


    private JPanel getRightPanel() {


        computePanel = new MultipleRoiComputePanel(this, getRaster());


        final JPanel rightPanel = GridBagUtils.createPanel();

        JTabbedPane tabbedPane = new JTabbedPane();


        final JPanel mainPane = GridBagUtils.createPanel();

        //  GridBagConstraints extendedOptionsPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1,insets.right=-2");
        GridBagConstraints extendedOptionsPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1");


        GridBagUtils.addToPanel(rightPanel, computePanel, extendedOptionsPanelConstraints, "gridy=0,fill=NONE,weighty=1,weightx=1");


        JPanel optionsPanel = getDisplayOptionsPanel();
        JPanel fieldsPanel = getFieldOptionsPanel();
        JPanel binningCriteriaPanel = getBinningCriteriaPanel();
        JPanel plotOptionsPanel = getPlotsOptionsPanel();
        JPanel viewPanel = getViewPanel();

        //    GridBagUtils.addToPanel(rightPanel, optionsPanel, extendedOptionsPanelConstraints, "gridy=1,fill=BOTH,weighty=0");

        //   GridBagUtils.addToPanel(rightPanel, binningCriteriaPanel, extendedOptionsPanelConstraints, "gridy=2,fill=BOTH,weighty=0");

        //  GridBagUtils.addToPanel(mainPane, computePanel, extendedOptionsPanelConstraints, "gridy=0,fill=NONE,weighty=1,weightx=1");
        GridBagUtils.addToPanel(mainPane, binningCriteriaPanel, extendedOptionsPanelConstraints, "gridy=1,fill=BOTH,weighty=0");


        tabbedPane.addTab("Bins", binningCriteriaPanel);
        tabbedPane.addTab("Fields", fieldsPanel);
        tabbedPane.addTab("Text", optionsPanel);
        tabbedPane.addTab("Plots", plotOptionsPanel);
        tabbedPane.addTab("View", viewPanel);

        GridBagUtils.addToPanel(rightPanel, tabbedPane, extendedOptionsPanelConstraints, "gridy=1,fill=BOTH,weighty=0, insets.top=10");


        exportButton = getExportButton();
        exportButton.setToolTipText("Export: This only exports the binning portion of the statistics");
        exportButton.setVisible(exportButtonVisible);

        final JPanel exportAndHelpPanel = GridBagUtils.createPanel();
        GridBagConstraints helpPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1,ipadx=0");
        GridBagUtils.addToPanel(exportAndHelpPanel, new JSeparator(), helpPanelConstraints, "fill=HORIZONTAL,gridwidth=2,insets.left=5,insets.right=5");
        GridBagUtils.addToPanel(exportAndHelpPanel, exportButton, helpPanelConstraints, "gridy=1,anchor=WEST,fill=NONE");
        GridBagUtils.addToPanel(exportAndHelpPanel, getHelpButton(), helpPanelConstraints, "gridx=1,gridy=1,anchor=EAST,fill=NONE");

        GridBagUtils.addToPanel(rightPanel, exportAndHelpPanel, extendedOptionsPanelConstraints, "gridy=2,anchor=SOUTHWEST,fill=HORIZONTAL,weighty=0,insets.top=0");


        rightPanel.setMinimumSize(rightPanel.getPreferredSize());

        return rightPanel;
    }


    @Override
    protected void updateComponents() {
        if (computePanel.isRunning()) {
            if (swingWorker != null) {
                swingWorker.cancel(true);
            }
            computePanel.setRunning(false);
        }


        if (!init) {
            initComponents();
        }

        plotsDomainLowTextfieldContainer.reset();
        plotsDomainHighTextfieldContainer.reset();

        statsSpreadsheet = null;


        final RasterDataNode raster = getRaster();
        computePanel.setRaster(raster);
        contentPanel.removeAll();
        spreadsheetPanel.removeAll();
        resultText.setLength(0);


        if (raster != null && raster.isStxSet() && raster.getStx().getResolutionLevel() == 0) {

            percentThresholdsList = getPercentThresholdsList();
            resultText.append(createText(raster.getStx(), null));
            contentPanel.add(createStatPanel(raster.getStx(), null, 1));
            spreadsheetPanel.add(statsSpreadsheetPanel());
            histograms = new Histogram[]{raster.getStx().getHistogram()};
            exportAsCsvAction = new ExportStatisticsAsCsvAction(this);
            putStatisticsIntoVectorDataAction = new PutStatisticsIntoVectorDataAction(this);
            exportButton.setEnabled(exportButtonEnabled);

        } else {
            contentPanel.add(new JLabel(DEFAULT_STATISTICS_TEXT));
            exportButton.setEnabled(false);
        }





        contentPanel.revalidate();
        contentPanel.repaint();
        spreadsheetScrollPane.setVisible(showStatsSpreadSheet);
        spreadsheetPanel.revalidate();
        spreadsheetPanel.repaint();
        backgroundPanel.revalidate();
        backgroundPanel.repaint();

        if (raster != null) {
            exportButton.setEnabled(false);
        }
    }

    @Override
    public Histogram[] getHistograms() {
        return histograms;
    }

    private static class ComputeResult {

        final Stx stx;
        final Mask mask;

        ComputeResult(Stx stx, Mask mask) {
            this.stx = stx;
            this.mask = mask;
        }
    }

    @Override
    public void compute(final Mask[] selectedMasks) {

        computePanel.setRunning(true);
        spreadsheetPanel.removeAll();
        fixedHistDomainAllPlotsInitialized = false;

        final int numHistograms = (computePanel.isIncludeUnmasked()) ? (selectedMasks.length + 1) : selectedMasks.length + 1;


        this.histograms = new Histogram[numHistograms];
        final String title = "Computing Statistics";

        percentThresholdsList = getPercentThresholdsList();

        if (percentThresholdsList == null) {
            abortRun();
            return;
        }

        statsSpreadsheet = null;  // reset this

        if (!retrieveValidateTextFields(true)) {
            abortRun();
            return;
        }

        // just in case: should not get here as it should have been caught earlier
        if (!validFields()) {
            JOptionPane.showMessageDialog(getParentDialogContentPane(),
                    "Failed to compute statistics due to invalid fields",
                    "Invalid Input", /*I18N*/
                    JOptionPane.ERROR_MESSAGE);
            computePanel.setRunning(false);
            return;
        }


         swingWorker = new ProgressMonitorSwingWorker(this, title) {

            //   SwingWorker<Object, ComputeResult> swingWorker = new ProgressMonitorSwingWorker<Object, ComputeResult>(this, title) {

            @Override
            protected Object doInBackground(ProgressMonitor pm) {
                pm.beginTask(title, selectedMasks.length);
                try {
                    //   final int binCount = Util.computeBinCount(accuracyModel.accuracy);        // TEST CODE generically preferred size of each column based on longest expected entry
                    // fails a bit because decimal formatting is not captured
                    // stub of code commented out in case we want to make it work
                    // meanwhile longest entry is being used SEE below


                    numProductRegions = numHistograms;


                    final int binCount = numBins;


                    int histogramIdx = 0;

                    if (computePanel.isIncludeUnmasked()) {
                        final Stx stx1;
                        ProgressMonitor subPm1 = SubProgressMonitor.create(pm, 1);


                        stx1 = new StxFactory()
                                .withHistogramBinCount(binCount)
                                .withLogHistogram(LogMode)
                                .withMedian(includeMedian)
                                .withBinMin(binMin)
                                .withBinMax(binMax)
                                .withBinWidth(binWidth)
                                .create(getRaster(), subPm1);


                        histograms[0] = stx1.getHistogram();

                        // todo Danny Testing this
                        // publish(new ComputeResult(stx1, null));


                        if (resultText.length() > 0) {
                            resultText.append("\n");
                        }
                        resultText.append(createText(stx1, null));

                        JPanel statPanel = createStatPanel(stx1, null, currRow);
                        contentPanel.add(statPanel);

                        double numRows = 1.0 + selectedMasks.length;

                        updateLeftPanel(numRows);

//                        contentPanel.revalidate();
//                        contentPanel.repaint();
//                        backgroundPanel.revalidate();
//                        backgroundPanel.repaint();
                        currRow++;

                        // end todo


                        histogramIdx++;
                    }


                    if (selectedMasks != null) {
                        for (int i = 0; i < selectedMasks.length; i++) {
                            final Mask mask = selectedMasks[i];

                            if (mask != null) {
                                final Stx stx;
                                ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);


                                stx = new StxFactory()
                                        .withHistogramBinCount(binCount)
                                        .withLogHistogram(LogMode)
                                        .withMedian(includeMedian)
                                        .withBinMin(binMin)
                                        .withBinMax(binMax)
                                        .withBinWidth(binWidth)
                                        .withRoiMask(mask)
                                        .create(getRaster(), subPm);


                                histograms[histogramIdx] = stx.getHistogram();


                                // todo Danny Testing this
                                // publish(new ComputeResult(stx, mask));


                                if (resultText.length() > 0) {
                                    resultText.append("\n");
                                }
                                resultText.append(createText(stx, mask));

                                JPanel statPanel = createStatPanel(stx, mask, currRow);
                                contentPanel.add(statPanel);


                                double numRows = 1.0 + selectedMasks.length;

                                updateLeftPanel(numRows);

//                                contentPanel.revalidate();
//                                contentPanel.repaint();
//                                backgroundPanel.revalidate();
//                                backgroundPanel.repaint();

                                currRow++;

                                // end todo

                                histogramIdx++;


                            }
                        }
                    }


                } finally {
                    pm.done();
                    computePanel.setRunning(false);

                }
                return null;
            }

            // todo Danny Testing this
//            @Override
//            protected void process(List<ComputeResult> chunks) {
//
//
//
//                for (ComputeResult result : chunks) {
//
//                    final Stx stx = result.stx;
//                    final Mask mask = result.mask;
//
//                    if (resultText.length() > 0) {
//                        resultText.append("\n");
//                    }
//                    resultText.append(createText(stx, mask));
//
//                    JPanel statPanel = createStatPanel(stx, mask, currRow);
//                    contentPanel.add(statPanel);
//                    contentPanel.revalidate();
//                    contentPanel.repaint();
//                    backgroundPanel.revalidate();
//                    backgroundPanel.repaint();
//                    currRow++;
//                }
//
//
//            }

            @Override
            protected void done() {
                try {

                    double numRows = 1.0 + selectedMasks.length;

                    updateLeftPanel(numRows);


                    get();
                    if (exportAsCsvAction == null) {
                        exportAsCsvAction = new ExportStatisticsAsCsvAction(StatisticsPanel.this);
                    }
                    exportAsCsvAction.setSelectedMasks(selectedMasks);
                    if (putStatisticsIntoVectorDataAction == null) {
                        putStatisticsIntoVectorDataAction = new PutStatisticsIntoVectorDataAction(StatisticsPanel.this);
                    }
                    putStatisticsIntoVectorDataAction.setSelectedMasks(selectedMasks);
                    //       exportButton.setEnabled(exportButtonEnabled);
//                    contentPanel.revalidate();
//                    contentPanel.repaint();
//                    backgroundPanel.revalidate();
//                    backgroundPanel.repaint();
                    currRow = 1;

                    computePanel.setRunning(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                            "Failed to compute statistics.\nAn error occurred:" + e.getMessage(),
                                                  /*I18N*/
                            "Statistics", /*I18N*/
                            JOptionPane.ERROR_MESSAGE);
                    computePanel.setRunning(false);
                }
            }
        };

        resultText.setLength(0);
        contentPanel.removeAll();

        // todo Danny Testing this
        // swingWorker.execute();
        swingWorker.executeWithBlocking();
    }


    private void abortRun() {
        initLeftPanel();
        fixedHistDomainAllPlotsInitialized = false;
        computePanel.setRunning(false);
    }


    private void initLeftPanel() {
        leftPanel.removeAll();
        leftPanel.add(new JLabel(DEFAULT_STATISTICS_TEXT));
        leftPanel.revalidate();
        leftPanel.repaint();
        // leftPanel.setBackground(Color.WHITE);
        fixedHistDomainAllPlotsInitialized = false;
    }

    private void updateLeftPanel(double numRows) {

        spreadsheetPanel.removeAll();
        JPanel statsSpeadPanel = statsSpreadsheetPanel();
        spreadsheetPanel.add(statsSpeadPanel);
        //   spreadsheetPanel.setBackground(Color.WHITE);
        spreadsheetScrollPane.setVisible(showStatsSpreadSheet);
        spreadsheetScrollPane.setMinimumSize(new Dimension(100, 100));


        leftPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        if (showPercentPlots || showHistogramPlots || showStatsList) {

            if (numRows > spreadsheetMinRowsBeforeWeight) {
                gbc.weighty = 1.0 - spreadsheetHeightWeight;
                leftPanel.add(contentScrollPane, gbc);

                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = spreadsheetHeightWeight;

            } else {
                leftPanel.add(contentScrollPane, gbc);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weighty = 0;

                int buffer = 50;
                int minHeight = spreadsheetPanel.getPreferredSize().height + buffer;
                spreadsheetScrollPane.setMinimumSize(new Dimension(100, minHeight));
            }

            gbc.gridy += 1;
            gbc.insets.top = 10;


        } else {
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;
        }

        if (showStatsSpreadSheet) {
            leftPanel.add(spreadsheetScrollPane, gbc);
        }

        leftPanel.revalidate();
        leftPanel.repaint();

        contentPanel.revalidate();
        contentPanel.repaint();
        backgroundPanel.revalidate();
        backgroundPanel.repaint();

    }


    private JPanel createStatPanel(Stx stx, final Mask mask, int row) {

        final Histogram histogram = stx.getHistogram();

        XIntervalSeries histogramSeries = new XIntervalSeries("Histogram");
        double histDomainBounds[] = {0, 0};
        double histRangeBounds[] = {0, 0};

        if (!fixedHistDomainAllPlots || (fixedHistDomainAllPlots && !fixedHistDomainAllPlotsInitialized)) {
            if (!LogMode) {
                if (plotsThreshDomainSpan && plotsThreshDomainLow >= 0.1 && plotsThreshDomainHigh <= 99.9) {
                    histDomainBounds[0] = histogram.getPTileThreshold((plotsThreshDomainLow) / 100)[0];
                    histDomainBounds[1] = histogram.getPTileThreshold(plotsThreshDomainHigh / 100)[0];
                } else if (plotsDomainSpan) {
                    histDomainBounds[0] = plotsDomainLow;
                    histDomainBounds[1] = plotsDomainHigh;
                }

            } else {
                histDomainBounds[0] = histogram.getBinLowValue(0, 0);
                histDomainBounds[1] = histogram.getHighValue(0);
            }


//            if (!LogMode && plotsThreshDomainSpan && plotsThreshDomainLow >= 0.1 && plotsThreshDomainHigh <= 99.9) {
//                histDomainBounds[0] = histogram.getPTileThreshold((plotsThreshDomainLow) / 100)[0];
//                histDomainBounds[1] = histogram.getPTileThreshold(plotsThreshDomainHigh / 100)[0];
//
//            } else {
//                histDomainBounds[0] = histogram.getBinLowValue(0, 0);
//                histDomainBounds[1] = histogram.getHighValue(0);
//            }

            if (fixedHistDomainAllPlots && !fixedHistDomainAllPlotsInitialized) {
                histDomainBoundsAllPlots[0] = histDomainBounds[0];
                histDomainBoundsAllPlots[1] = histDomainBounds[1];
                fixedHistDomainAllPlotsInitialized = true;
            }
        } else {
            histDomainBounds[0] = histDomainBoundsAllPlots[0];
            histDomainBounds[1] = histDomainBoundsAllPlots[1];
        }


        int[] bins = histogram.getBins(0);
        for (int j = 0; j < bins.length; j++) {

            histogramSeries.add(histogram.getBinLowValue(0, j),
                    histogram.getBinLowValue(0, j),
                    j < bins.length - 1 ? histogram.getBinLowValue(0, j + 1) : histogram.getHighValue(0),
                    bins[j]);
        }


        String logTitle = (LogMode) ? "Log10 of " : "";
        ChartPanel histogramPanel = createChartPanel(histogramSeries,
                logTitle + getRaster().getName() + " (" + getRaster().getUnit() + ")",
                "Frequency in #Pixels",
                new Color(0, 0, 127),
                histDomainBounds, histRangeBounds);


        //  histogramPanel.setPreferredSize(new Dimension(300, 200));


        if (exactPlotSize) {
            histogramPanel.setMinimumSize(new Dimension(plotSizeWidth, plotSizeHeight));
            histogramPanel.setPreferredSize(new Dimension(plotSizeWidth, plotSizeHeight));
            histogramPanel.setMaximumSize(new Dimension(plotSizeWidth, plotSizeHeight));
        } else {
            histogramPanel.setMinimumSize(new Dimension(plotMinWidth, plotMinHeight));
            histogramPanel.setPreferredSize(new Dimension(plotMinWidth, plotMinHeight));
        }

        XIntervalSeries percentileSeries = new XIntervalSeries("Percentile");

//        if (1 == 2 && LogMode) {
//            percentileSeries.add(0,
//                    0,
//                    1,
//                    Math.pow(10, histogram.getLowValue(0)));
//            for (int j = 1; j < 99; j++) {
//                percentileSeries.add(j,
//                        j,
//                        j + 1,
//                        Math.pow(10, histogram.getPTileThreshold(j / 100.0)[0]));
//            }
//            percentileSeries.add(99,
//                    99,
//                    100,
//                    Math.pow(10, histogram.getHighValue(0)));
//
//        } else {
//            percentileSeries.add(0,
//                    0,
//                    0.25,
//                    histogram.getLowValue(0));
//
//            for (double j = 0.25; j < 99.75; j += .25) {
//                percentileSeries.add(j,
//                        j,
//                        j + 1,
//                        histogram.getPTileThreshold(j / 100.0)[0]);
//            }
//            percentileSeries.add(99.75,
//                    99.75,
//                    100,
//                    histogram.getHighValue(0));
//        }


//
//        double fraction = 0;
//        for (int j = 0; j < bins.length; j++) {
//
//             fraction = (1.0) * j / bins.length;
//
//            if (fraction > 0 && fraction < 1) {
//                percentileSeries.add(histogram.getBinLowValue(0, j),
//                        histogram.getBinLowValue(0, j),
//                        j < bins.length - 1 ? histogram.getBinLowValue(0, j + 1) : histogram.getHighValue(0),
//                        histogram.getPTileThreshold(fraction)[0]);
//            }
//
//
//        }
//
//        double test = fraction;


        double[] percentileDomainBounds = {0, 0};
        double[] percentileRangeBounds = {0, 0};
        ChartPanel percentilePanel = null;

        if (invertPercentile) {

            double increment = .01;
            for (double j = 0; j < 100; j += increment) {
                double fraction = j / 100.0;
                double nextFraction = (j + increment) / 100.0;

                if (fraction > 0.0 && fraction < 1.0 && nextFraction > 0.0 && nextFraction < 1.0) {
                    double thresh = histogram.getPTileThreshold(fraction)[0];
                    double nextThresh = histogram.getPTileThreshold(nextFraction)[0];

                    percentileSeries.add(thresh,
                            thresh,
                            nextThresh,
                            j);
                }
            }


            if (!LogMode) {
                percentileDomainBounds[0] = histDomainBounds[0];
                percentileDomainBounds[1] = histDomainBounds[1];
            }
            percentileRangeBounds[0] = 0;
            percentileRangeBounds[1] = 100;

            percentilePanel = createScatterChartPanel(percentileSeries, logTitle + getRaster().getName() + " (" + getRaster().getUnit() + ")", "Percent Threshold", new Color(0, 0, 0), percentileDomainBounds, percentileRangeBounds);

        } else {
            percentileSeries.add(0,
                    0,
                    0.25,
                    histogram.getLowValue(0));

            for (double j = 0.25; j < 99.75; j += .25) {
                percentileSeries.add(j,
                        j,
                        j + 1,
                        histogram.getPTileThreshold(j / 100.0)[0]);
            }
            percentileSeries.add(99.75,
                    99.75,
                    100,
                    histogram.getHighValue(0));


            percentileDomainBounds[0] = 0;
            percentileDomainBounds[1] = 100;
            percentileRangeBounds[0] = histDomainBounds[0];
            percentileRangeBounds[1] = histDomainBounds[1];

            percentilePanel = createScatterChartPanel(percentileSeries, "Percent Threshold", logTitle + getRaster().getName() + " (" + getRaster().getUnit() + ")", new Color(0, 0, 0), percentileDomainBounds, percentileRangeBounds);


        }

        //   percentilePanel.setPreferredSize(new Dimension(300, 200));
        if (exactPlotSize) {
            percentilePanel.setMinimumSize(new Dimension(plotSizeWidth, plotSizeHeight));
            percentilePanel.setPreferredSize(new Dimension(plotSizeWidth, plotSizeHeight));
            percentilePanel.setMaximumSize(new Dimension(plotSizeWidth, plotSizeHeight));
        } else {
            percentilePanel.setMinimumSize(new Dimension(plotMinWidth, plotMinHeight));
            percentilePanel.setPreferredSize(new Dimension(plotMinWidth, plotMinHeight));
        }

        int size = getRaster().getRasterHeight() * getRaster().getRasterWidth();


        int dataRows = 0;

        Object[][] firstData =
                new Object[][]{
                        new Object[]{"RasterSize(Pixels)", size},
                        new Object[]{"SampleSize(Pixels)", histogram.getTotals()[0]},
                        new Object[]{"Minimum", stx.getMinimum()},
                        new Object[]{"Maximum", stx.getMaximum()},
                        new Object[]{"Mean", stx.getMean()}
                };
        dataRows += firstData.length;


        Object[] medianObject = null;

        if (includeMedian) {
            medianObject = new Object[]{"Median", stx.getMedianRaster()};

            dataRows++;
        }


        Object[][] secondData =
                new Object[][]{
                        new Object[]{"StandardDeviation", stx.getStandardDeviation()},
                        new Object[]{"CoefficientOfVariation", getCoefficientOfVariation(stx)},
                        //     new Object[]{"", ""},
                        new Object[]{"TotalBins", histogram.getNumBins()[0]},
                        new Object[]{"BinWidth", getBinSize(histogram)}
                };
        dataRows += secondData.length;


        Object[][] histogramStats = null;
        if (includeHistogramStats) {
            if (LogMode) {
                histogramStats = new Object[][]{
                        new Object[]{"Mean(LogBinned)", Math.pow(10, histogram.getMean()[0])},
                        new Object[]{"Median(LogBinned)", Math.pow(10, stx.getMedian())},
                        new Object[]{"StandardDeviation(LogBinned)", Math.pow(10, histogram.getStandardDeviation()[0])}
                };
            } else {
                histogramStats = new Object[][]{
                        new Object[]{"Mean(Binned)", histogram.getMean()[0]},
                        new Object[]{"Median(Binned)", stx.getMedian()},
                        new Object[]{"StandardDeviation(Binned)", histogram.getStandardDeviation()[0]}
                };
            }
            dataRows += histogramStats.length;
        }


        Object[][] percentData = new Object[percentThresholdsList.size()][];
        for (int i = 0; i < percentThresholdsList.size(); i++) {
            int value = percentThresholdsList.get(i);
            double percent = value / 100.0;
            String percentString = Integer.toString(value);

            Object[] pTileThreshold;
            if (LogMode) {
                pTileThreshold = new Object[]{percentString + "%Threshold(LogBinned)", Math.pow(10, histogram.getPTileThreshold(percent)[0])};
            } else {
                pTileThreshold = new Object[]{percentString + "%Threshold(Binned)", histogram.getPTileThreshold(percent)[0]};
            }
            percentData[i] = pTileThreshold;
        }
        dataRows += percentData.length;


        Object[][] tableData = new Object[dataRows][];
        int tableDataIdx = 0;

        if (firstData != null) {
            for (int i = 0; i < firstData.length; i++) {
                tableData[tableDataIdx] = firstData[i];
                tableDataIdx++;
            }
        }

        if (medianObject != null) {
            tableData[tableDataIdx] = medianObject;
            tableDataIdx++;
        }

        if (secondData != null) {
            for (int i = 0; i < secondData.length; i++) {
                tableData[tableDataIdx] = secondData[i];
                tableDataIdx++;
            }
        }


        if (histogramStats != null) {
            for (int i = 0; i < histogramStats.length; i++) {
                tableData[tableDataIdx] = histogramStats[i];
                tableDataIdx++;
            }
        }

        if (percentData != null) {
            for (int i = 0; i < percentData.length; i++) {
                //  int tableDataIndex = i + firstData.length;
                tableData[tableDataIdx] = percentData[i];
                tableDataIdx++;
            }
        }


        if (statsSpreadsheet == null) {
            numStatisticFields = tableData.length;

            int numCols = numStatisticFields + 2; // Add 2 cols "Region" and "Band"
            int numRows = numProductRegions + 1; // Add header row

            statsSpreadsheet = new Object[numRows][numCols];
        }


        // Add Headers
        if (row <= 1) {
            statsSpreadsheet[0][0] = "Region";
            statsSpreadsheet[0][1] = "Band";


            for (int i = 0; i < tableData.length; i++) {
                Object value = tableData[i][0];

                int k = i + 2;
                if (k < statsSpreadsheet[0].length) {
                    statsSpreadsheet[0][k] = value;
                }
            }
        }


        if (row < statsSpreadsheet.length) {
            statsSpreadsheet[row][0] = (mask != null) ? mask.getName() : "";
            statsSpreadsheet[row][1] = getRaster().getName();
            for (int i = 0; i < tableData.length; i++) {
                Object value = tableData[i][1];

                int k = i + 2;
                if (k < statsSpreadsheet[row].length) {
                    statsSpreadsheet[row][k] = value;
                }
            }
        }

        // todo Danny stats spreadsheet too big

        int numPlots = 0;
        if (showPercentPlots) {
            numPlots++;
        }

        if (showHistogramPlots) {
            numPlots++;
        }

        JPanel plotContainerPanel = null;

        if (numPlots > 0) {
            plotContainerPanel = new JPanel(new GridLayout(1, numPlots));

            if (showHistogramPlots) {
                plotContainerPanel.add(histogramPanel);
            }

            if (showPercentPlots) {
                plotContainerPanel.add(percentilePanel);
            }
        }


        TableModel tableModel = new DefaultTableModel(tableData, new String[]{"Name", "Value"}) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : Number.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };


        final JTable table = new JTable(tableModel);
        table.setDefaultRenderer(Number.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Float || value instanceof Double) {
                    setHorizontalTextPosition(RIGHT);
                    setText(getFormattedValue((Number) value));
                }
                return label;
            }

            private String getFormattedValue(Number value) {
                if (value.doubleValue() < 0.001 && value.doubleValue() > -0.001 && value.doubleValue() != 0.0) {
                    return new DecimalFormat("0.####E0").format(value.doubleValue());
                }
                String format = null;
                if (decimalPlaces == DECIMAL_PLACES_FULL) {
                    format = "%f";
//                } else if (decimalPlaces == 0) {
//                    format = "%0f";
                } else {
                    format = "%." + Integer.toString(decimalPlaces) + "f";
                }
                return String.format(format, value.doubleValue());
                // return String.format("%.4f", value.doubleValue());
            }
        });
        table.addMouseListener(popupHandler);


        // TEST CODE generically preferred size of each column based on longest expected entry
        // fails a bit because decimal formatting is not captured
        // stub of code commented out in case we want to make it work
        // meanwhile longest entry is being used SEE below

//        int column0Length = 0;
//        int column1Length = 0;
//        FontMetrics fm = table.getFontMetrics(table.getFont());
//        for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
//            String test = table.getValueAt(rowIndex,0).toString();
//            int currColumn0Length = fm.stringWidth(table.getValueAt(rowIndex,0).toString());
//            if (currColumn0Length > column0Length) {
//                column0Length = currColumn0Length;
//            }
//
//            String test2 = table.getValueAt(rowIndex,1).toString();
//            int currColumn1Length = fm.stringWidth(table.getValueAt(rowIndex,1).toString());
//            if (currColumn1Length > column1Length) {
//                column1Length = currColumn1Length;
//            }
//        }


        // Set preferred size of each column based on longest expected entry
        FontMetrics fm = table.getFontMetrics(table.getFont());
        TableColumn column = null;
        int col1PreferredWidth = -1;
        if (LogMode) {
            col1PreferredWidth = fm.stringWidth("StandardDeviation(LogBinned):") + 10;
        } else {
            col1PreferredWidth = fm.stringWidth("StandardDeviation(Binned):") + 10;
        }


        // int col1PreferredWidth = fm.stringWidth("wwwwwwwwwwwwwwwwwwwwwwwwww");
        int col2PreferredWidth = fm.stringWidth("1234567890") + 10;
        int tablePreferredWidth = col1PreferredWidth + col2PreferredWidth;
        for (int i = 0; i < 2; i++) {
            column = table.getColumnModel().getColumn(i);
            if (i == 0) {
                column.setPreferredWidth(col1PreferredWidth);
                column.setMaxWidth(col1PreferredWidth);
            } else {
                column.setPreferredWidth(col2PreferredWidth);
            }
        }


        JPanel textContainerPanel = new JPanel(new BorderLayout(2, 2));
        //   textContainerPanel.setBackground(Color.WHITE);
        textContainerPanel.add(table, BorderLayout.CENTER);

        JPanel statsPane = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints("");
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.weighty = 1;

        Dimension dim = table.getPreferredSize();
        table.setPreferredSize(new Dimension(tablePreferredWidth, dim.height));
        statsPane.add(table, gbc);
        statsPane.setPreferredSize(new Dimension(tablePreferredWidth, dim.height));

        JPanel plotsPane = null;

        if (plotContainerPanel != null) {
            plotsPane = GridBagUtils.createPanel();
            plotsPane.setBackground(Color.WHITE);
            //    plotsPane.setBorder(UIUtils.createGroupBorder(" ")); /*I18N*/
            GridBagConstraints gbcPlots = GridBagUtils.createConstraints("");
            gbcPlots.gridy = 0;
            if (exactPlotSize) {
                gbcPlots.fill = GridBagConstraints.NONE;
            } else {
                gbcPlots.fill = GridBagConstraints.BOTH;
            }

            gbcPlots.anchor = GridBagConstraints.NORTHWEST;
            gbcPlots.weightx = 0.5;
            gbcPlots.weighty = 1;
            plotsPane.add(plotContainerPanel, gbcPlots);
        }


        JPanel mainPane = GridBagUtils.createPanel();
        mainPane.setBorder(UIUtils.createGroupBorder(getSubPanelTitle(mask))); /*I18N*/
        GridBagConstraints gbcMain = GridBagUtils.createConstraints("");
        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.anchor = GridBagConstraints.NORTHWEST;
        if (plotsPane != null) {
            gbcMain.fill = GridBagConstraints.VERTICAL;
            gbcMain.weightx = 0;
        } else {
            gbcMain.fill = GridBagConstraints.BOTH;
            gbcMain.weightx = 1;
        }

        if (showStatsList) {
            gbcMain.weighty = 1;
            mainPane.add(statsPane, gbcMain);
            gbcMain.gridx++;
        }


        gbcMain.weightx = 1;
        gbcMain.weighty = 1;
        gbcMain.fill = GridBagConstraints.BOTH;


        if (plotsPane != null) {
            mainPane.add(plotsPane, gbcMain);
        }


        return mainPane;
    }

    static double getBinSize(Histogram histogram) {
        return (histogram.getHighValue(0) - histogram.getLowValue(0)) / histogram.getNumBins(0);
    }

    static double getBinSizeLogMode(Histogram histogram) {
        return (Math.pow(10, histogram.getHighValue(0)) - Math.pow(10, histogram.getLowValue(0))) / histogram.getNumBins(0);
    }

    private String getSubPanelTitle(Mask mask) {
        final String title;
        if (mask != null) {
            title = String.format("<html><b>%s</b> with ROI-mask <b>%s</b></html>", getRaster().getName(), mask.getName());
        } else {
            title = String.format("<html><b>%s</b></html>", getRaster().getName());
        }
        return title;
    }

    @Override
    protected String getDataAsText() {
        return resultText.toString();
    }


    // todo This part has not been updated by Danny
    private String createText(final Stx stx, final Mask mask) {

        if (stx.getSampleCount() == 0) {
            if (mask != null) {
                return "The ROI-Mask '" + mask.getName() + "' is empty.";
            } else {
                return "The scene contains no valid pixels.";
            }
        }

        RasterDataNode raster = getRaster();
        boolean maskUsed = mask != null;
        final String unit = (StringUtils.isNotNullAndNotEmpty(raster.getUnit()) ? raster.getUnit() : "1");
        final long numPixelTotal = (long) raster.getSceneRasterWidth() * (long) raster.getSceneRasterHeight();
        final StringBuilder sb = new StringBuilder(1024);

        sb.append("Only ROI-mask pixels considered:\t");
        sb.append(maskUsed ? "Yes" : "No");
        sb.append("\n");

        if (maskUsed) {
            sb.append("ROI-mask name:\t");
            sb.append(mask.getName());
            sb.append("\n");
        }

        sb.append("Number of pixels total:\t");
        sb.append(numPixelTotal);
        sb.append("\n");

        sb.append("Number of considered pixels:\t");
        sb.append(stx.getSampleCount());
        sb.append("\n");

        sb.append("Ratio of considered pixels:\t");
        sb.append(100.0 * stx.getSampleCount() / numPixelTotal);
        sb.append("\t");
        sb.append("%");
        sb.append("\n");

        sb.append("Minimum:\t");
        sb.append(stx.getMinimum());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Maximum:\t");
        sb.append(stx.getMaximum());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Mean:\t");
        sb.append(stx.getMean());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Standard deviation:\t");
        sb.append(stx.getStandardDeviation());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Coefficient of variation:\t");
        sb.append(getCoefficientOfVariation(stx));
        sb.append("\t");
        sb.append("");
        sb.append("\n");

        sb.append("Bin Median:\t");
        sb.append(stx.getMedianRaster());
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        for (int percentile = 5; percentile <= 95; percentile += 5) {
            sb.append("P").append(percentile).append(" threshold:\t");
            sb.append(stx.getHistogram().getPTileThreshold(percentile / 100.0)[0]);
            sb.append("\t");
            sb.append(unit);
            sb.append("\n");
        }

        sb.append("Threshold max error:\t");
        sb.append(getBinSize(stx.getHistogram()));
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        return sb.toString();
    }

    private double getCoefficientOfVariation(Stx stx) {
        return stx.getStandardDeviation() / stx.getMean();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        backgroundPanel.setBounds(0, 0, getWidth() - 8, getHeight() - 8);
        hideAndShowButton.setBounds(getWidth() - hideAndShowButton.getWidth() - 12, 6, 24, 24);
    }


    private static ChartPanel createChartPanel(XIntervalSeries percentileSeries, String xAxisLabel, String yAxisLabel, Color color, double domainBounds[], double rangeBounds[]) {
        XIntervalSeriesCollection percentileDataset = new XIntervalSeriesCollection();
        percentileDataset.addSeries(percentileSeries);
        return getHistogramPlotPanel(percentileDataset, xAxisLabel, yAxisLabel, color, domainBounds, rangeBounds);
    }

    private static ChartPanel createScatterChartPanel(XIntervalSeries percentileSeries, String xAxisLabel, String yAxisLabel, Color color, double domainBounds[], double rangeBounds[]) {
        XIntervalSeriesCollection percentileDataset = new XIntervalSeriesCollection();
        percentileDataset.addSeries(percentileSeries);
        return getScatterPlotPanel(percentileDataset, xAxisLabel, yAxisLabel, color, domainBounds, rangeBounds);
    }

    private static ChartPanel getHistogramPlotPanel(XIntervalSeriesCollection dataset, String xAxisLabel, String yAxisLabel, Color color, double domainBounds[], double rangeBounds[]) {
        JFreeChart chart = ChartFactory.createHistogram(
                null,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                false,  // Legend?
                true,   // tooltips
                false   // url
        );
        final XYPlot xyPlot = chart.getXYPlot();
        //xyPlot.setForegroundAlpha(0.85f);
        xyPlot.setNoDataMessage("No data");
        xyPlot.setAxisOffset(new RectangleInsets(5, 5, 5, 10));
        // xyPlot.setInsets(new RectangleInsets(0,0,0,0));

        // todo Danny set bounds here

        if (domainBounds[0] != domainBounds[1]) {
            xyPlot.getDomainAxis().setLowerBound(domainBounds[0]);
            xyPlot.getDomainAxis().setUpperBound(domainBounds[1]);
        }

        if (rangeBounds[0] != rangeBounds[1]) {
            xyPlot.getRangeAxis().setLowerBound(rangeBounds[0]);
            xyPlot.getRangeAxis().setUpperBound(rangeBounds[1]);
        }


        final XYBarRenderer renderer = (XYBarRenderer) xyPlot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setSeriesPaint(0, color);
        StandardXYBarPainter painter = new StandardXYBarPainter();
        renderer.setBarPainter(painter);

        ChartPanel chartPanel = new ChartPanel(chart);
//// todo Danny testing out height/width ratio preservation
//        double histChartHeightWidthRatio = chartPanel.getPreferredSize().height / chartPanel.getPreferredSize().width;
//        double plotSizeReduction = 1;
//        Number preferredHeight = chartPanel.getPreferredSize().height * plotSizeReduction;
//        Number preferredWidth = chartPanel.getPreferredSize().width * plotSizeReduction;
//
//        chartPanel.setPreferredSize(new Dimension(preferredWidth.intValue(), preferredHeight.intValue()));

        //  chartPanel.setPreferredSize(new Dimension(300, 200));

//        chartPanel.getPopupMenu().add(createCopyDataToClipboardMenuItem());
        return chartPanel;
    }

    private static ChartPanel getScatterPlotPanel(XIntervalSeriesCollection dataset, String xAxisLabel, String yAxisLabel, Color color, double domainBounds[], double rangeBounds[]) {
        //  JFreeChart chart = ChartFactory.createScatterPlot(
        JFreeChart chart = ChartFactory.createXYLineChart(
                null,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                false,  // Legend?
                true,   // tooltips
                false   // url
        );
        final XYPlot xyPlot = chart.getXYPlot();
        //   xyPlot.setForegroundAlpha(0.85f);
        xyPlot.setBackgroundAlpha(0.0f);
        xyPlot.setNoDataMessage("No data");
        xyPlot.setAxisOffset(new RectangleInsets(5, 5, 5, 10));


        // todo Danny set bounds here

        if (domainBounds[0] != domainBounds[1]) {
            xyPlot.getDomainAxis().setLowerBound(domainBounds[0]);
            xyPlot.getDomainAxis().setUpperBound(domainBounds[1]);
        }

        if (rangeBounds[0] != rangeBounds[1]) {
            xyPlot.getRangeAxis().setLowerBound(rangeBounds[0]);
            xyPlot.getRangeAxis().setUpperBound(rangeBounds[1]);
        }

        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) xyPlot.getRenderer();
        renderer.setSeriesPaint(0, color);
        renderer.setUseFillPaint(true);
        renderer.setDrawOutlines(true);
        renderer.setSeriesShapesFilled(0, true);
        renderer.setSeriesFillPaint(0, color);


        ChartPanel chartPanel = new ChartPanel(chart);
        //    chartPanel.setPreferredSize(new Dimension(300, 200));

        return chartPanel;
    }


    private AbstractButton getExportButton() {
        final AbstractButton export = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Export24.gif"),
                false);
        export.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPopupMenu viewPopup = new JPopupMenu("Export");
                viewPopup.add(exportAsCsvAction);
                viewPopup.add(putStatisticsIntoVectorDataAction);
                final Rectangle buttonBounds = export.getBounds();
                viewPopup.show(export, 1, buttonBounds.height + 1);
            }
        });
        export.setEnabled(false);
        return export;
    }

    @Override
    public RasterDataNode getRasterDataNode() {
        return getRaster();
    }

    @Override
    public ProductNodeGroup<VectorDataNode> getVectorDataNodeGroup() {
        return getRasterDataNode().getProduct().getVectorDataGroup();
    }

    private class PopupHandler extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == 2 || e.isPopupTrigger()) {
                final JPopupMenu menu = new JPopupMenu();
                menu.add(createCopyDataToClipboardMenuItem());
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    // The fields of this class are used by the binding framework
    @SuppressWarnings("UnusedDeclaration")
    static class AccuracyModel {

        private int accuracy = 3;
        private boolean useAutoAccuracy = true;
    }


    private void textfieldHandler(final JTextField textField) {

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                textfieldHandlerAction(textField);
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                textfieldHandlerAction(textField);
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                textfieldHandlerAction(textField);
            }
        });
    }

    private void textfieldHandlerAction(final JTextField textField) {

        if (handlersEnabled) {
            if (StatisticsToolView.PARAM_LABEL_PERCENT_THRESHOLDS.equals(textField.getName())) {
                percentThresholds = textField.getText().toString();
            }
        }

    }


    private boolean validNumBins() {
        if (numBins < StatisticsToolView.PARAM_MINVAL_NUM_BINS || numBins > StatisticsToolView.PARAM_MAXVAL_NUM_BINS) {
            return false;
        }

        return true;
    }


    private boolean retrieveValidateTextFields(boolean showDialog) {
        if (!validNumBins()) {
            return false;
        }


        exactPlotSize = plotsSizeCheckBox.isSelected();
        plotsDomainSpan = plotsDomainSpanCheckBox.isSelected();
        plotsThreshDomainSpan = plotsThreshDomainSpanCheckBox.isSelected();

        if (plotsSizeCheckBox.isSelected()) {
            if (plotsSizeHeightTextfieldContainer != null && plotsSizeHeightTextfieldContainer.isValid(true) && plotsSizeHeightTextfieldContainer.getValue() != null) {
                plotSizeHeight = plotsSizeHeightTextfieldContainer.getValue().intValue();
            } else {
                return false;
            }

            if (plotsSizeWidthTextfieldContainer != null && plotsSizeWidthTextfieldContainer.isValid(true) && plotsSizeWidthTextfieldContainer.getValue() != null) {
                plotSizeWidth = plotsSizeWidthTextfieldContainer.getValue().intValue();
            } else {
                return false;
            }
        } else {
            plotSizeHeight = StatisticsToolView.PARAM_DEFVAL_PLOTS_SIZE_HEIGHT;
            plotSizeWidth = StatisticsToolView.PARAM_DEFVAL_PLOTS_SIZE_WIDTH;
        }


        if (plotsDomainSpanCheckBox.isSelected()) {
            if (plotsDomainLowTextfieldContainer != null && plotsDomainLowTextfieldContainer.isValid(true) && plotsDomainLowTextfieldContainer.getValue() != null) {
                plotsDomainLow = plotsDomainLowTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }


            if (plotsDomainHighTextfieldContainer != null && plotsDomainHighTextfieldContainer.isValid(true) && plotsDomainHighTextfieldContainer.getValue() != null) {
                plotsDomainHigh = plotsDomainHighTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }
        } else {
            plotsDomainLow = StatisticsToolView.PARAM_DEFVAL_PLOTS_DOMAIN_LOW;
            plotsDomainHigh = StatisticsToolView.PARAM_DEFVAL_PLOTS_DOMAIN_HIGH;
        }


        if (plotsThreshDomainSpanCheckBox.isSelected()) {
            if (plotsThreshDomainLowTextfieldContainer != null && plotsThreshDomainLowTextfieldContainer.isValid(true) && plotsThreshDomainLowTextfieldContainer.getValue() != null) {
                plotsThreshDomainLow = plotsThreshDomainLowTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }


            if (plotsThreshDomainHighTextfieldContainer != null && plotsThreshDomainHighTextfieldContainer.isValid(true) && plotsThreshDomainHighTextfieldContainer.getValue() != null) {
                plotsThreshDomainHigh = plotsThreshDomainHighTextfieldContainer.getValue().doubleValue();
            } else {
                return false;
            }
        } else {
            plotsThreshDomainLow = StatisticsToolView.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_LOW;
            plotsThreshDomainHigh = StatisticsToolView.PARAM_DEFVAL_PLOTS_THRESH_DOMAIN_HIGH;
        }


        if (numBinsTextfieldContainer != null && numBinsTextfieldContainer.isValid(true) && numBinsTextfieldContainer.getValue() != null) {
            numBins = numBinsTextfieldContainer.getValue().intValue();
        } else {
            return false;
        }

        if (spreadsheetColWidthTextfieldContainer != null && spreadsheetColWidthTextfieldContainer.isValid(true) && spreadsheetColWidthTextfieldContainer.getValue() != null) {
            colCharWidth = spreadsheetColWidthTextfieldContainer.getValue().intValue();
        } else {
            return false;
        }


        {
            String lowName = plotsThreshDomainLowTextfieldContainer.getName();
            String highName = plotsThreshDomainHighTextfieldContainer.getName();
            double lowVal = plotsThreshDomainLowTextfieldContainer.getValue().doubleValue();
            double highVal = plotsThreshDomainHighTextfieldContainer.getValue().doubleValue();
            if (!compareFields(lowVal, highVal, lowName, highName, true)) {
                return false;
            }
        }

        {
            String lowName = plotsDomainLowTextfieldContainer.getName();
            String highName = plotsDomainHighTextfieldContainer.getName();
            double lowVal = plotsDomainLowTextfieldContainer.getValue().doubleValue();
            double highVal = plotsDomainHighTextfieldContainer.getValue().doubleValue();
            if (!compareFields(lowVal, highVal, lowName, highName, true)) {
                return false;
            }
        }


        return true;
    }


    private boolean compareFields(double lowVal, double highVal, String lowName, String HighName, boolean showDialog) {
        if (lowVal >= highVal) {
            if (showDialog) {
                JOptionPane.showMessageDialog(getParentDialogContentPane(),
                        "ERROR: Value of " + lowName + " must be greater than value of " + HighName,
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            }

            return false;
        }

        return true;

    }


    private boolean validFields() {
        if (!validNumBins()) {
            return false;
        }

        return true;
    }

    private List<Integer> getPercentThresholdsList() {
        List<Integer> percentThresholdsList = new ArrayList<Integer>();

        String[] thresholds = percentThresholds.split(",");


        for (String threshold : thresholds) {
            if (threshold != null) {
                threshold.trim();
                if (threshold.length() > 0) {
                    int value;
                    try {
                        value = Integer.parseInt(threshold);
                        if (value < 0 || value > 100) {
                            JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                    "ERROR: Valid " + StatisticsToolView.PARAM_LABEL_PERCENT_THRESHOLDS + " range is (0 to 100)",
                                    "Invalid Input",
                                    JOptionPane.ERROR_MESSAGE);
                            return null;

                        } else {
                            percentThresholdsList.add(value);
                        }
                    } catch (NumberFormatException exception) {
                        JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                StatisticsToolView.PARAM_LABEL_PERCENT_THRESHOLDS + "field " + exception.toString(),
                                "Invalid Input",
                                JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                }
            }
        }


        return percentThresholdsList;
    }

    private JPanel statsSpreadsheetPanel() {

        JPanel pane = GridBagUtils.createPanel();

        if (statsSpreadsheet == null) {
            return pane;
        }
        //     pane.setBorder(UIUtils.createGroupBorder("Statistics Spreadsheet")); /*I18N*/
        GridBagConstraints gbcMain = GridBagUtils.createConstraints("");
        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.weighty = 1.0;
        gbcMain.anchor = GridBagConstraints.NORTHWEST;




        TableModel tableModel = new DefaultTableModel(statsSpreadsheet, statsSpreadsheet[0]) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex <= 1 ? String.class : Number.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };


        final JTable table = new JTable(tableModel);
        table.setDefaultRenderer(Number.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Float || value instanceof Double) {
                    setHorizontalTextPosition(RIGHT);
                    setText(getFormattedValue((Number) value));
                }
                return label;
            }

            private String getFormattedValue(Number value) {
                if (value.doubleValue() < 0.001 && value.doubleValue() > -0.001 && value.doubleValue() != 0.0) {
                    return new DecimalFormat("0.####E0").format(value.doubleValue());
                }
                String format = null;
                if (decimalPlaces == DECIMAL_PLACES_FULL) {
                    format = "%f";
//                } else if (decimalPlaces == 0) {
//                    format = "%0f";
                } else {
                    format = "%." + Integer.toString(decimalPlaces) + "f";
                }
                return String.format(format, value.doubleValue());
                // return String.format("%.4f", value.doubleValue());
            }
        });
        table.addMouseListener(popupHandler);


        FontMetrics fm = table.getFontMetrics(table.getFont());
        TableColumn column = null;

        // int colPreferredWidthData = fm.stringWidth("StandardDeviation(LogBinned):") + 10;

        StringBuilder sampleEntry = new StringBuilder("");
        for (int i = 1; i < colCharWidth; i++) {
            sampleEntry.append("n");
        }

        int colPreferredWidthData = fm.stringWidth(sampleEntry.toString());
        int tableWidth = 0;
        int bufferWidth = fm.stringWidth("nn");


        for (int i = 0; i < statsSpreadsheet[0].length; i++) {
            column = table.getColumnModel().getColumn(i);

            if (colCharWidth < 8) {
                String header = statsSpreadsheet[0][i].toString();
                int headerWidth = fm.stringWidth(header) + bufferWidth;

                column.setPreferredWidth(headerWidth);
                column.setMaxWidth(headerWidth);
                column.setMinWidth(headerWidth);
                tableWidth += headerWidth;
            } else {
                column.setPreferredWidth(colPreferredWidthData);
                column.setMaxWidth(colPreferredWidthData);
                column.setMinWidth(colPreferredWidthData);
                tableWidth += colPreferredWidthData;
            }
        }


        //  table.setPreferredSize(new Dimension(tableWidth, table.getRowCount() * table.getRowHeight()));

        pane.add(table, gbcMain);
        //  pane.setMinimumSize(new Dimension(tableWidth, table.getRowCount() * table.getRowHeight()));


        return pane;
    }


    public boolean getPreferencesHistogramPlotEnabled() {

        if (configuration != null) {
            return configuration.getPropertyBool(StatisticsToolView.PARAM_KEY_HISTOGRAM_PLOT_ENABLED, StatisticsToolView.PARAM_DEFVAL_HISTOGRAM_PLOT_ENABLED);
        } else {
            return StatisticsToolView.PARAM_DEFVAL_HISTOGRAM_PLOT_ENABLED;
        }
    }

    public boolean getPreferencesPercentPlotEnabled() {

        if (configuration != null) {
            return configuration.getPropertyBool(StatisticsToolView.PARAM_KEY_PERCENT_PLOT_ENABLED, StatisticsToolView.PARAM_DEFVAL_PERCENT_PLOT_ENABLED);
        } else {
            return StatisticsToolView.PARAM_DEFVAL_PERCENT_PLOT_ENABLED;
        }
    }

    public boolean getPreferencesStatsListEnabled() {

        if (configuration != null) {
            return configuration.getPropertyBool(StatisticsToolView.PARAM_KEY_STATS_LIST_ENABLED, StatisticsToolView.PARAM_DEFVAL_STATS_LIST_ENABLED);
        } else {
            return StatisticsToolView.PARAM_DEFVAL_STATS_LIST_ENABLED;
        }
    }

    public boolean getPreferencesStatsSpreadSheetEnabled() {

        if (configuration != null) {
            return configuration.getPropertyBool(StatisticsToolView.PARAM_KEY_STATS_SPREADSHEET_ENABLED, StatisticsToolView.PARAM_DEFVAL_STATS_SPREADSHEET_ENABLED);
        } else {
            return StatisticsToolView.PARAM_DEFVAL_STATS_SPREADSHEET_ENABLED;
        }
    }


    public String getPreferencesPercentThresholds() {

        if (configuration != null) {
            return configuration.getPropertyString(StatisticsToolView.PARAM_KEY_PERCENT_THRESHOLDS, StatisticsToolView.PARAM_DEFVAL_PERCENT_THRESHOLDS);
        } else {
            return StatisticsToolView.PARAM_DEFVAL_PERCENT_THRESHOLDS;
        }
    }


    public int getPreferencesNumBins() {

        if (configuration != null) {
            return configuration.getPropertyInt(StatisticsToolView.PARAM_KEY_NUM_BINS, StatisticsToolView.PARAM_DEFVAL_NUM_BINS);
        } else {
            return StatisticsToolView.PARAM_DEFVAL_NUM_BINS;
        }
    }

    void updateEnablement() {
//        boolean hasMasks = (product != null && product.getMaskGroup().getNodeCount() > 0);
//        boolean canSelectMasks = hasMasks && useRoiCheckBox.isSelected();
//        useRoiCheckBox.setEnabled(hasMasks);
//        maskNameSearchField.setEnabled(canSelectMasks);
//        maskNameList.setEnabled(canSelectMasks);
//        selectAllCheckBox.setEnabled(canSelectMasks && maskNameList.getCheckBoxListSelectedIndices().length < maskNameList.getModel().getSize());
//        selectNoneCheckBox.setEnabled(canSelectMasks && maskNameList.getCheckBoxListSelectedIndices().length > 0);
//        if (!isRunning()) {
//            refreshButton.setEnabled(raster != null);
//        }
    }


}




