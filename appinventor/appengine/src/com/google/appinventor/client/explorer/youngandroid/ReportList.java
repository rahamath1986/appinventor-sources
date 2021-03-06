// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.explorer.youngandroid;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.appinventor.client.GalleryClient;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.OdeMessages;
import com.google.appinventor.client.widgets.DropDownButton;
import com.google.appinventor.client.widgets.DropDownButton.DropDownItem;
import com.google.appinventor.shared.rpc.project.GalleryAppReport;
import com.google.appinventor.shared.rpc.project.GalleryModerationAction;
import com.google.appinventor.shared.rpc.project.Message;
import com.google.appinventor.shared.rpc.user.User;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * The report list shows all reports in a table.
 *
 * <p> The report text, date created, user reported on and user reporting will be shown in the table.
 *
 * @author wolberd@gmail.com, based on ProjectList.java, lizlooney@google.com (Liz Looney),
 * @author blu2@dons.usfca.edu (Bin Lu)
 */
public class ReportList extends Composite  {
  public static final int MAX_MESSAGE_PREVIEW_LENGTH = 40;
  private final CheckBox checkBox;
  private final VerticalPanel panel;
  private List<GalleryAppReport> reports;
  private List<GalleryAppReport> selectedReports;
  private final List<GalleryAppReport> selectedGalleryAppReports;
  private final Map<GalleryAppReport, ReportWidgets> ReportWidgets;
  private DropDownButton templateButton;
  private GalleryClient galleryClient;

  // UI elements
  private final Grid table;

  public static final OdeMessages MESSAGES = GWT.create(OdeMessages.class);

  public static final int MESSAGE_INAPPROPRIATE_APP_CONTENT_REMOVE = 1;
  public static final int MESSAGE_INAPPROPRIATE_APP_CONTENT = 2;
  public static final int MESSAGE_INAPPROPRIATE_USER_PROFILE_CONTENT = 3;

  /**
   * Creates a new ProjectList
   */
  public ReportList() {
    galleryClient = GalleryClient.getInstance();
    // Initialize UI
    panel = new VerticalPanel();
    panel.setWidth("100%");

    HorizontalPanel checkBoxPanel = new HorizontalPanel();
    checkBoxPanel.addStyleName("all-reports");
    checkBox = new CheckBox();
    checkBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        boolean isChecked = event.getValue(); // auto-unbox from Boolean to boolean
        if (isChecked) {
          initializeAllReports();
        } else {
          initializeReports();
        }
      }
    });
    checkBoxPanel.add(checkBox);
    Label checkBoxText = new Label(MESSAGES.moderationShowResolvedReports());
    checkBoxPanel.add(checkBoxText);
    panel.add(checkBoxPanel);

    selectedGalleryAppReports = new ArrayList<GalleryAppReport>();
    ReportWidgets = new HashMap<GalleryAppReport, ReportWidgets>();

    table = new Grid(1, 9); // The table initially contains just the header row.
    table.addStyleName("ode-ModerationTable");
    table.setWidth("100%");
    table.setCellSpacing(0);

    setHeaderRow();

    panel.add(table);
    initWidget(panel);

    initializeReports();

  }

  /**
   * Adds the header row to the table.
   *
   */
  private void setHeaderRow() {
    table.getRowFormatter().setStyleName(0, "ode-ProjectHeaderRow");

    HorizontalPanel reportHeader = new HorizontalPanel();
    final Label reportHeaderLabel = new Label(MESSAGES.moderationReportTextHeader());
    reportHeaderLabel.addStyleName("ode-ProjectHeaderLabel");
    reportHeader.add(reportHeaderLabel);
    table.setWidget(0, 0, reportHeader);

    HorizontalPanel appHeader = new HorizontalPanel();
    final Label appHeaderLabel = new Label(MESSAGES.moderationAppHeader());
    appHeaderLabel.addStyleName("ode-ProjectHeaderLabel");
    appHeader.add(appHeaderLabel);
    table.setWidget(0, 1, appHeader);

    HorizontalPanel dateCreatedHeader = new HorizontalPanel();
    final Label dateCreatedHeaderLabel = new Label(MESSAGES.moderationReportDateCreatedHeader());
    dateCreatedHeaderLabel.addStyleName("ode-ProjectHeaderLabel");
    dateCreatedHeader.add(dateCreatedHeaderLabel);
    table.setWidget(0, 2, dateCreatedHeader);

    HorizontalPanel appAuthorHeader = new HorizontalPanel();
    final Label appAuthorHeaderLabel = new Label(MESSAGES.moderationAppAuthorHeader());
    appAuthorHeaderLabel.addStyleName("ode-ProjectHeaderLabel");
    appAuthorHeader.add(appAuthorHeaderLabel);
    table.setWidget(0, 3, appAuthorHeader);

    HorizontalPanel reporterHeader = new HorizontalPanel();
    final Label reporterHeaderLabel = new Label(MESSAGES.moderationReporterHeader());
    reporterHeaderLabel.addStyleName("ode-ProjectHeaderLabel");
    reporterHeader.add(reporterHeaderLabel);
    table.setWidget(0, 4, reporterHeader);

  }

  /**
   * initialize reports, only including solved reports
   */
  private void initializeReports() {
    final OdeAsyncCallback<List<GalleryAppReport>> callback = new OdeAsyncCallback<List<GalleryAppReport>>(
      // failure message
      MESSAGES.galleryError()) {
        @Override
        public void onSuccess(List<GalleryAppReport> reportList) {
          reports=reportList;
          ReportWidgets.clear();
          for (GalleryAppReport report : reports) {
            ReportWidgets.put(report, new ReportWidgets(report));
          }
          refreshTable();
        }
    };
    Ode.getInstance().getGalleryService().getRecentReports(0,10,callback);
  }

  /**
   * initialize all reports, including both solved and unsolved reports
   */
  private void initializeAllReports() {
    final OdeAsyncCallback<List<GalleryAppReport>> callback = new OdeAsyncCallback<List<GalleryAppReport>>(
      // failure message
      MESSAGES.galleryError()) {
        @Override
        public void onSuccess(List<GalleryAppReport> reportList) {
          reports=reportList;
          ReportWidgets.clear();
          for (GalleryAppReport report : reports) {
            ReportWidgets.put(report, new ReportWidgets(report));
          }
          refreshTable();
        }
      };
    Ode.getInstance().getGalleryService().getAllAppReports(0,10,callback);
  }
  /**
   * Helper wrapper Class of Report Widgets
   */
  private class ReportWidgets {
    final Label reportTextLabel;
    final Label appLabel;
    final Label dateCreatedLabel;
    final Label appAuthorlabel;
    final Label reporterLabel;
    final Button sendMessageButton;
    final Button deactiveAppButton;
    final Button markAsResolvedButton;
    final Button seeAllActions;
    boolean appActive;
    boolean appResolved;
    /**
     * Constructor of ReportWidgets
     * @param report GalleryAppReport
     */
    private ReportWidgets(final GalleryAppReport report) {

      reportTextLabel = new Label(report.getReportText());
      reportTextLabel.addStyleName("ode-ProjectNameLabel");

      appLabel = new Label(report.getApp().getTitle());
      appLabel.addStyleName("primary-link");

      DateTimeFormat dateTimeFormat = DateTimeFormat.getMediumDateTimeFormat();
      Date dateCreated = new Date(report.getTimeStamp());
      dateCreatedLabel = new Label(dateTimeFormat.format(dateCreated));

      appAuthorlabel = new Label(report.getOffender().getUserName());
      appAuthorlabel.addStyleName("primary-link");

      reporterLabel = new Label(report.getReporter().getUserName());
      reporterLabel.addStyleName("primary-link");

      sendMessageButton = new Button(MESSAGES.buttonSendMessage());

      deactiveAppButton = new Button(MESSAGES.labelDeactivateApp());

      markAsResolvedButton = new Button(MESSAGES.labelmarkAsResolved());

      seeAllActions = new Button(MESSAGES.labelSeeAllActions());
    }
  }

  /**
   * refresh report list table
   * Update the information of reports
   */
  private void refreshTable() {

    // Refill the table.
    table.resize(1 + reports.size(), 9);
    int row = 1;
    for (GalleryAppReport report : reports) {
      ReportWidgets rw = ReportWidgets.get(report);
      table.setWidget(row, 0, rw.reportTextLabel);
      table.setWidget(row, 1, rw.appLabel);
      table.setWidget(row, 2, rw.dateCreatedLabel);
      table.setWidget(row, 3, rw.appAuthorlabel);
      table.setWidget(row, 4, rw.reporterLabel);
      table.setWidget(row, 5, rw.sendMessageButton);
      table.setWidget(row, 6, rw.deactiveAppButton);
      table.setWidget(row, 7, rw.markAsResolvedButton);
      table.setWidget(row, 8, rw.seeAllActions);
      prepareGalleryAppReport(report, rw);
      row++;
    }

    Ode.getInstance().getProjectToolbar().updateButtons();
  }

  /**
   * Prepare gallery app report based on given GalleryAppReport
   * Setup the functionality of UI components.
   * @param r GalleryAppReport gallery app report
   * @param rw ReportWidgets report widgets
   */
  private void prepareGalleryAppReport(final GalleryAppReport r, final ReportWidgets rw) {
    rw.reportTextLabel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {

      }
    });

    rw.appLabel.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Ode.getInstance().switchToGalleryAppView(r.getApp(), GalleryPage.VIEWAPP);
        }
    });

    rw.appAuthorlabel.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Ode.getInstance().switchToUserProfileView(r.getOffender().getUserId(), 1 /* 1 for public view*/ );
        }
    });

    rw.reporterLabel.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
            Ode.getInstance().switchToUserProfileView(r.getReporter().getUserId(), 1 /* 1 for public view*/ );
        }
    });

    rw.sendMessageButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        sendMessagePopup(r);
      }
    });

    final OdeAsyncCallback<Boolean> isActivatedCallback = new OdeAsyncCallback<Boolean>(
    // failure message
    MESSAGES.galleryError()) {
      @Override
      public void onSuccess(Boolean active) {
        if(active){
          rw.deactiveAppButton.setText(MESSAGES.labelDeactivateApp());
          rw.appActive = true;
        }
        else {
          rw.deactiveAppButton.setText(MESSAGES.labelReactivateApp());
          rw.appActive = false;
        }
      }
    };
    Ode.getInstance().getGalleryService().isGalleryAppActivated(r.getApp().getGalleryAppId(), isActivatedCallback);

    rw.deactiveAppButton.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if(rw.appActive == true){
              deactivateAppPopup(r, rw);
          }else{

              final OdeAsyncCallback<Boolean> callback = new OdeAsyncCallback<Boolean>(
              MESSAGES.galleryError()) {
                @Override
                public void onSuccess(Boolean success) {
                  if(!success)
                    return;
                  rw.deactiveAppButton.setText(MESSAGES.labelDeactivateApp());//revert button
                  rw.appActive = true;
                  storeModerationAction(r.getReportId(), r.getApp().getGalleryAppId(), GalleryModerationAction.NOTAVAILABLE,
                      GalleryModerationAction.REACTIVATEAPP, null);
                  //update gallery list
                  galleryClient.appWasChanged();
                }
              };
              Ode.getInstance().getGalleryService().deactivateGalleryApp(r.getApp().getGalleryAppId(), callback);
          }
        }
    });

    if(r.getResolved()){                                                //report was unresolved, now resolved
      rw.markAsResolvedButton.setText(MESSAGES.labelmarkAsUnresolved());//revert button
      rw.appResolved = true;
    }else{                                                              //report was resolved, now unresolved
      rw.markAsResolvedButton.setText(MESSAGES.labelmarkAsResolved());  //revert button
      rw.appResolved = false;
    }
    rw.markAsResolvedButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        final OdeAsyncCallback<Boolean> callback = new OdeAsyncCallback<Boolean>(
          // failure message
          MESSAGES.galleryError()) {
            @Override
            public void onSuccess(Boolean success) {
              if(success){
                if(r.getResolved()){//current status was resolved
                  r.setResolved(false);
                  rw.markAsResolvedButton.setText(MESSAGES.labelmarkAsResolved());//revert button
                  rw.appResolved = false;
                  storeModerationAction(r.getReportId(), r.getApp().getGalleryAppId(), GalleryModerationAction.NOTAVAILABLE,
                      GalleryModerationAction.MARKASUNRESOLVED, null);
                }else{//current status was unResolved
                  r.setResolved(true);
                  rw.markAsResolvedButton.setText(MESSAGES.labelmarkAsUnresolved());//revert button
                  rw.appResolved = true;
                  storeModerationAction(r.getReportId(), r.getApp().getGalleryAppId(), GalleryModerationAction.NOTAVAILABLE,
                      GalleryModerationAction.MARKASRESOLVED, null);
                }
                if(checkBox.getValue() == false){//only unresolved reports, remove directly.
                  onReportRemoved(r);
                }
              }
            }
          };
        Ode.getInstance().getGalleryService().markReportAsResolved(r.getReportId(), r.getApp().getGalleryAppId(), callback);
      }
    });

    rw.seeAllActions.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        //show actions
        seeAllActionsPopup(r);
      }
    });
  }

  /**
   * Gets the number of reports
   *
   * @return the number of reports
   */
  public int getNumGalleryAppReports() {
    return reports.size();
  }

  /**
   * Gets the number of selected reports
   *
   * @return the number of selected reports
   */
  public int getNumSelectedGalleryAppReports() {
    return selectedGalleryAppReports.size();
  }

  /**
   * Returns the list of selected reports
   *
   * @return the selected reports
   */
  public List<GalleryAppReport> getSelectedGalleryAppReports() {
    return selectedGalleryAppReports;
  }
  /**
   * Method when added gallery app report
   * @param report GalleryAppReport galleryapp report
   */
  public void onReportAdded(GalleryAppReport report) {
    reports.add(report);
    ReportWidgets.put(report, new ReportWidgets(report));
    refreshTable();
  }
  /**
   * Method when removed gallery app report
   * @param report GalleryAppReport galleryapp report
   */
  public void onReportRemoved(GalleryAppReport report) {
    reports.remove(report);
    ReportWidgets.remove(report);
    refreshTable();
    selectedGalleryAppReports.remove(report);
  }
  /**
   * Helper method of creating a sending message popup
   * @param report
   */
  private void sendMessagePopup(final GalleryAppReport report){
      // Create a PopUpPanel with a button to close it
      final PopupPanel popup = new PopupPanel(true);
      popup.setStyleName("ode-InboxContainer");
      final FlowPanel content = new FlowPanel();
      content.addStyleName("ode-Inbox");
      Label title = new Label(MESSAGES.messageSendTitle());
      title.addStyleName("InboxTitle");
      content.add(title);

      Button closeButton = new Button(MESSAGES.symbolX());
      closeButton.addStyleName("CloseButton");
      closeButton.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          popup.hide();
        }
      });
      content.add(closeButton);

      final FlowPanel msgPanel = new FlowPanel();
      msgPanel.addStyleName("app-actions");
      final Label sentFrom = new Label(MESSAGES.messageSentFrom());
      final Label sentTo = new Label(MESSAGES.messageSentTo() + report.getOffender().getUserName());
      final TextArea msgText = new TextArea();
      msgText.addStyleName("action-textarea");
      final Button sendMsg = new Button(MESSAGES.buttonSendMessage());
      sendMsg.addStyleName("action-button");

      // Account Drop Down Button
      List<DropDownItem> templateItems = Lists.newArrayList();
      // Messages Template 1
      templateItems.add(new DropDownItem("template1", MESSAGES.inappropriateAppContentRemoveTitle(), new TemplateAction(msgText, MESSAGE_INAPPROPRIATE_APP_CONTENT_REMOVE, report.getApp().getTitle())));
      templateItems.add(new DropDownItem("template2", MESSAGES.inappropriateAppContentTitle(), new TemplateAction(msgText, MESSAGE_INAPPROPRIATE_APP_CONTENT, report.getApp().getTitle())));
      templateItems.add(new DropDownItem("template3", MESSAGES.inappropriateUserProfileContentTitle(), new TemplateAction(msgText, MESSAGE_INAPPROPRIATE_USER_PROFILE_CONTENT, null)));

      templateButton = new DropDownButton("template", MESSAGES.labelChooseTemplate(), templateItems, true);
      templateButton.setStyleName("ode-TopPanelButton");

      new TemplateAction(msgText, MESSAGE_INAPPROPRIATE_APP_CONTENT, report.getApp().getTitle()).execute();

      msgPanel.add(templateButton);
      msgPanel.add(sentFrom);
      msgPanel.add(sentTo);
      msgPanel.add(msgText);
      msgPanel.add(sendMsg);

      content.add(msgPanel);
      popup.setWidget(content);
      // Center and show the popup
      popup.center();

      final User currentUser = Ode.getInstance().getUser();
      sentFrom.setText(MESSAGES.messageSentFrom() + currentUser.getUserName());
      sendMsg.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          final OdeAsyncCallback<Long> messagesCallback = new OdeAsyncCallback<Long>(
            MESSAGES.galleryError()) {
              @Override
              public void onSuccess(final Long msgId) {
                popup.hide();
                storeModerationAction(report.getReportId(), report.getApp().getGalleryAppId(), msgId,
                    GalleryModerationAction.SENDMESSAGE, getMessagePreview(msgText.getText()));
              }
            };
            Ode.getInstance().getGalleryService().sendMessageFromSystem(
                currentUser.getUserId(), report.getOffender().getUserId(),
                msgText.getText(), messagesCallback);
        }
      });
  }
  /**
   * Helper method for deactivating App Popup
   * @param report GalleryAppReport Gallery App Report
   * @param rw ReportWidgets Report Widgets
   */
  private void deactivateAppPopup(final GalleryAppReport report, final ReportWidgets rw){
      // Create a PopUpPanel with a button to close it
      final PopupPanel popup = new PopupPanel(true);
      popup.setStyleName("ode-InboxContainer");
      final FlowPanel content = new FlowPanel();
      content.addStyleName("ode-Inbox");
      Label title = new Label(MESSAGES.messageSendTitle());
      title.addStyleName("InboxTitle");
      content.add(title);

      Button closeButton = new Button(MESSAGES.symbolX());
      closeButton.addStyleName("CloseButton");
      closeButton.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          popup.hide();
        }
      });
      content.add(closeButton);

      final FlowPanel msgPanel = new FlowPanel();
      msgPanel.addStyleName("app-actions");
      final Label sentFrom = new Label(MESSAGES.messageSentFrom());
      final Label sentTo = new Label(MESSAGES.messageSentTo() + report.getOffender().getUserName());
      final TextArea msgText = new TextArea();
      msgText.addStyleName("action-textarea");
      final Button sendMsgAndDRApp = new Button(MESSAGES.labelDeactivateAppAndSendMessage());
      sendMsgAndDRApp.addStyleName("action-button");
      final Button cancel = new Button(MESSAGES.labelCancel());
      cancel.addStyleName("action-button");

      // Account Drop Down Button
      List<DropDownItem> templateItems = Lists.newArrayList();
      // Messages Template 1
      templateItems.add(new DropDownItem("template1", MESSAGES.inappropriateAppContentRemoveTitle(), new TemplateAction(msgText, MESSAGE_INAPPROPRIATE_APP_CONTENT_REMOVE, report.getApp().getTitle())));
      templateItems.add(new DropDownItem("template2", MESSAGES.inappropriateAppContentTitle(), new TemplateAction(msgText, MESSAGE_INAPPROPRIATE_APP_CONTENT, report.getApp().getTitle())));
      templateItems.add(new DropDownItem("template3", MESSAGES.inappropriateUserProfileContentTitle(), new TemplateAction(msgText, MESSAGE_INAPPROPRIATE_USER_PROFILE_CONTENT, null)));
      templateButton = new DropDownButton("template", MESSAGES.labelChooseTemplate(), templateItems, true);
      templateButton.setStyleName("ode-TopPanelButton");

      // automatically choose first template
      new TemplateAction(msgText, MESSAGE_INAPPROPRIATE_APP_CONTENT_REMOVE, report.getApp().getTitle()).execute();

      msgPanel.add(templateButton);
      msgPanel.add(sentFrom);
      msgPanel.add(sentTo);
      msgPanel.add(msgText);
      msgPanel.add(sendMsgAndDRApp);
      msgPanel.add(cancel);

      content.add(msgPanel);
      popup.setWidget(content);
      // Center and show the popup
      popup.center();

      cancel.addClickHandler(new ClickHandler() {
          public void onClick(ClickEvent event) {
            popup.hide();
          }
      });

      final User currentUser = Ode.getInstance().getUser();
      sentFrom.setText(MESSAGES.messageSentFrom() + currentUser.getUserName());
      sendMsgAndDRApp.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          final OdeAsyncCallback<Long> messagesCallback = new OdeAsyncCallback<Long>(
            MESSAGES.galleryError()) {
              @Override
              public void onSuccess(final Long msgId) {
                popup.hide();

                final OdeAsyncCallback<Boolean> callback = new OdeAsyncCallback<Boolean>(
                  // failure message
                  MESSAGES.galleryError()) {
                    @Override
                      public void onSuccess(Boolean success) {
                        if(!success)
                          return;
                        popup.hide();
                        if(rw.appActive == true){                                     //app was active, now is deactive
                          rw.deactiveAppButton.setText(MESSAGES.labelReactivateApp());//revert button
                          rw.appActive = false;
                          storeModerationAction(report.getReportId(), report.getApp().getGalleryAppId(), msgId,
                              GalleryModerationAction.DEACTIVATEAPP, getMessagePreview(msgText.getText()));
                        }else{                                                        //app was deactive, now is active
                          /*This should not be reached, just in case*/
                          rw.deactiveAppButton.setText(MESSAGES.labelDeactivateApp());//revert button
                          rw.appActive = true;
                          storeModerationAction(report.getReportId(), report.getApp().getGalleryAppId(), msgId,
                              GalleryModerationAction.REACTIVATEAPP, getMessagePreview(msgText.getText()));
                        }
                        //update gallery list
                        galleryClient.appWasChanged();
                      }
                   };
                Ode.getInstance().getGalleryService().deactivateGalleryApp(report.getApp().getGalleryAppId(), callback);
              }
            };
            Ode.getInstance().getGalleryService().sendMessageFromSystem(currentUser.getUserId(), report.getOffender().getUserId(), msgText.getText(), messagesCallback);
        }
      });
    }

  /**
   * Helper method of creating popup window to show all associated moderation actions.
   * @param report GalleryAppReport gallery app report
   */
  private void seeAllActionsPopup(GalleryAppReport report){
    // Create a PopUpPanel with a button to close it
    final PopupPanel popup = new PopupPanel(true);
    popup.setStyleName("ode-InboxContainer");
    final FlowPanel content = new FlowPanel();
    content.addStyleName("ode-Inbox");
    Label title = new Label(MESSAGES.titleSeeAllActionsPopup());
    title.addStyleName("InboxTitle");
    content.add(title);

    Button closeButton = new Button(MESSAGES.symbolX());
    closeButton.addStyleName("CloseButton");
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        popup.hide();
      }
    });
    content.add(closeButton);

    final FlowPanel actionPanel = new FlowPanel();
    actionPanel.addStyleName("app-actions");

    final OdeAsyncCallback<List<GalleryModerationAction>> callback = new OdeAsyncCallback<List<GalleryModerationAction>>(
      // failure message
      MESSAGES.galleryError()) {
        @Override
          public void onSuccess(List<GalleryModerationAction> moderationActions) {
            for(final GalleryModerationAction moderationAction : moderationActions){
              FlowPanel record = new FlowPanel();
              Label time = new Label();
              Date createdDate = new Date(moderationAction.getDate());
              DateTimeFormat dateFormat = DateTimeFormat.getFormat("yyyy/MM/dd HH:mm:ss");
              time.setText(dateFormat.format(createdDate));
              time.addStyleName("time-label");
              record.add(time);
              Label moderatorLabel = new Label();
              moderatorLabel.setText(moderationAction.getModeratorName());
              moderatorLabel.addStyleName("moderator-link");
              moderatorLabel.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  Ode.getInstance().switchToUserProfileView(moderationAction.getModeratorId(), 1 /* 1 for public view*/ );
                  popup.hide();
                }
              });
              record.add(moderatorLabel);
              final Label actionLabel = new Label();
              actionLabel.addStyleName("inline-label");
              record.add(actionLabel);
              int actionType= moderationAction.getActonType();
              switch(actionType){
                case GalleryModerationAction.SENDMESSAGE:
                  actionLabel.setText(MESSAGES.moderationActionSendAMessage());
                  createMessageCollapse(record, moderationAction.getMesaageId(),  moderationAction.getMessagePreview());
                  break;
                case GalleryModerationAction.DEACTIVATEAPP:
                  actionLabel.setText(MESSAGES.moderationActionDeactivateThisAppWithMessage());
                  createMessageCollapse(record, moderationAction.getMesaageId(),  moderationAction.getMessagePreview());
                  break;
                case GalleryModerationAction.REACTIVATEAPP:
                  actionLabel.setText(MESSAGES.moderationActionReactivateThisApp());
                  break;
                case GalleryModerationAction.MARKASRESOLVED:
                  actionLabel.setText(MESSAGES.moderationActionMarkThisReportAsResolved());
                  break;
                case GalleryModerationAction.MARKASUNRESOLVED:
                  actionLabel.setText(MESSAGES.moderationActionMarkThisReportAsUnresolved());
                  break;
                default:
                  break;
              }
              actionPanel.add(record);
            }
          }
       };
    Ode.getInstance().getGalleryService().getModerationActions(report.getReportId(), callback);

    content.add(actionPanel);
    popup.setWidget(content);
    // Center and show the popup
    popup.center();
  }
  /**
   * Helper class for message template action
   * Choose Message Template based on given type
   */
  private class TemplateAction implements Command {
    TextArea msgText;
    int type;
    String customText;
    /**
     *
     * @param msgText message textarea UI
     * @param type default message type
     * @param customText moderator custom text
     */
    TemplateAction(TextArea msgText, int type, String customText){
      this.msgText = msgText;
      this.type = type;
      this.customText = customText;
    }
    @Override
    public void execute() {
      if(type == MESSAGE_INAPPROPRIATE_APP_CONTENT_REMOVE){
        msgText.setText(MESSAGES.yourAppMessage() + "\"" + customText  + MESSAGES.inappropriateAppContentRemoveMessage());
        templateButton.setCaption(MESSAGES.inappropriateAppContentRemoveTitle());
      }else if(type == MESSAGE_INAPPROPRIATE_APP_CONTENT){
         msgText.setText(MESSAGES.yourAppMessage() + "\""  + customText + MESSAGES.inappropriateAppContentMessage());
        templateButton.setCaption(MESSAGES.inappropriateAppContentTitle());
      }else if(type == MESSAGE_INAPPROPRIATE_USER_PROFILE_CONTENT){
        msgText.setText(MESSAGES.inappropriateUserProfileContentMessage());
        templateButton.setCaption(MESSAGES.inappropriateUserProfileContentTitle());
      }
    }
  }
  /**
   * Store Moderation Action into database
   * @param reportId report id
   * @param galleryId gallery id
   * @param messageId message id
   * @param actionType action type
   * @param messagePreview message preview
   */
  void storeModerationAction(final long reportId, final long galleryId, final long messageId, final int actionType, final String messagePreview){
    final User currentUser = Ode.getInstance().getUser();
    final OdeAsyncCallback<Void> moderationActionCallback = new OdeAsyncCallback<Void>(
      // failure message
      MESSAGES.galleryError()) {
        @Override
        public void onSuccess(Void result) {

        }
    };
    Ode.getInstance().getGalleryService().storeModerationAction(reportId, galleryId, messageId, currentUser.getUserId(),
        actionType, currentUser.getUserName(), messagePreview, moderationActionCallback);
  }
  /**
   * Help method for Message Collapse Function
   * When the button(see more) is clicked, it will retrieve the whole message from database.
   * @param parent the parent container
   * @param msgId message id
   * @param preview message preview
   */
  void createMessageCollapse(final FlowPanel parent, final long msgId, final String preview){
    final Label messageContent = new Label();
    messageContent.setText(preview);
    messageContent.addStyleName("inline-label");
    parent.add(messageContent);
    final Label actionButton = new Label();
    actionButton.setText(MESSAGES.seeMoreLink());
    actionButton.addStyleName("seemore-link");
    parent.add(actionButton);
    if(preview.length() <= MAX_MESSAGE_PREVIEW_LENGTH){
      actionButton.setVisible(false);
    }
    actionButton.addClickHandler(new ClickHandler() {
      boolean ifPreview = true;
      @Override
      public void onClick(ClickEvent event) {
        if(ifPreview == true){
          OdeAsyncCallback<Message> callback = new OdeAsyncCallback<Message>(
              // failure message
              MESSAGES.serverUnavailable()) {
                @Override
                public void onSuccess(final Message message) {
                  messageContent.setText(message.getMessage());
                  messageContent.addStyleName("inline");
                  actionButton.setText(MESSAGES.hideLink());
                  ifPreview = false;
                }
              };
          Ode.getInstance().getGalleryService().getMessage(msgId, callback);
        }else{
          messageContent.setText(preview);
          actionButton.setText(MESSAGES.seeMoreLink());
          ifPreview = true;
        }
      }
    });
  }
  /**
   * prune the message based on MAX_MESSAGE_PREVIEW_LENGTH.
   * If the message is longer than MAX_MESSAGE_PREVIEW_LENGTH,
   * The rest of it will save as "..."
   * @param message the origin message
   * @return a message preview
   */
  String getMessagePreview(String message){
    if(message != null && message.length() > MAX_MESSAGE_PREVIEW_LENGTH){
      return message.substring(0, MAX_MESSAGE_PREVIEW_LENGTH) + MESSAGES.moderationDotDotDot();
    }else{
      return message;
    }
  }
}