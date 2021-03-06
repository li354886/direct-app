/*
 * Copyright (C) 2011 - 2012 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.direct.services.view.action.contest;

import com.topcoder.direct.services.configs.ConfigUtils;
import com.topcoder.direct.services.view.action.contest.launch.DirectStrutsActionsHelper;
import com.topcoder.direct.services.view.action.contest.launch.StudioOrSoftwareContestAction;
import com.topcoder.direct.services.view.dto.TcJiraIssue;
import com.topcoder.direct.services.view.dto.UserProjectsDTO;
import com.topcoder.direct.services.view.dto.contest.ContestIssuesTrackingDTO;
import com.topcoder.direct.services.view.dto.contest.ContestStatsDTO;
import com.topcoder.direct.services.view.dto.contest.TypedContestBriefDTO;
import com.topcoder.direct.services.view.dto.project.ProjectBriefDTO;
import com.topcoder.direct.services.view.util.DataProvider;
import com.topcoder.direct.services.view.util.DirectUtils;
import com.topcoder.direct.services.view.util.SessionData;
import com.topcoder.direct.services.view.util.SessionFileStore;
import com.topcoder.direct.services.view.util.jira.JiraRpcServiceWrapper;
import com.topcoder.security.TCSubject;
import com.topcoder.service.facade.contest.ContestServiceFacade;
import com.topcoder.service.project.SoftwareCompetition;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Action class which handles retrieving Jira issues and bug races for the contest</p>
 *
 * <p>Version 1.1 (TC Cockpit Bug Tracking R1 Cockpit Project Tracking version 1.0) change notes:
 * - refactor the logic of getting ContestIssuesTrackingDTO into method DataProvider.getContestIssues
 * </p>
 *
 * <p>Version 1.2 (TC Direct Contest Dashboard Update Assembly version 1.0) change notes:
 * - update executeAction method to set contest dashboard data.
 * </p>
 *
 * <p>
 * Version 1.3 (Release Assembly - TC Direct Issue Tracking Tab Update Assembly 2 v1.0) change notes:
 *   <ol>
 *     <li>Update {@link #executeAction()} to get the last closed final fix phase and clear the 
 *     temporary attachments in the <code>SessionFileStore</code>.</li>
 *   </ol>
 * </p>
 * <p>
 * Version 1.4 (Release Assembly - TopCoder Cockpit Software Checkpoint Management) Change notes:
 *   <ol>
 *     <li>Updated {@link #executeAction()} method to add parameter softwareCompetition when calling
 *     updated method {@link DirectUtils#getContestStats(TCSubject, long, SoftwareCompetition)}.</li>
 *   </ol>
 * </p>
 *
 * @author Veve, TCSASSEMBLER
 * @version 1.4
 */
public class ContestIssuesTrackingAction extends StudioOrSoftwareContestAction {

    /**
     * <p>A <code>SessionData</code> providing interface to current session.</p>
     */
    private SessionData sessionData;

    /**
     * <p>A <code>ContestIssuesTrackingDTO</code> providing the view data for displaying by
     * <code>Contest issues and bug races</code> view. </p>
     */
    private ContestIssuesTrackingDTO viewData;

    /**
     * Gets the view data for this action.
     *
     * @return the view data for this action.
     */
    public ContestIssuesTrackingDTO getViewData() {
        return viewData;
    }

    /**
     * Initialize the action. The constructor will initialize an empty view data.
     */
    public ContestIssuesTrackingAction() {
        this.viewData = new ContestIssuesTrackingDTO();
    }

    /**
     * <p>Handles the incoming request. If action is executed successfully then changes the current project context to
     * project for contest requested for this action.</p>
     *
     * <p>Changes in version 1.1: the logic of getting and populating ContestIssuesTrackingDTO
     * has been refactored into method DataProvider.getContestIssues.</p>
     *
     * @throws Exception if an unexpected error occurs while processing the request.
     */
    @Override
    public void executeAction() throws Exception {
        // Get current session
        HttpServletRequest request = DirectUtils.getServletRequest();
        this.sessionData = new SessionData(request.getSession());

        ContestServiceFacade contestServiceFacade = getContestServiceFacade();
        TCSubject currentUser = DirectStrutsActionsHelper.getTCSubjectFromSession();

        // Set registrants data
        long contestId = getProjectId();

        SoftwareCompetition competition = contestServiceFacade.getSoftwareContestByProjectId(currentUser, contestId);
        boolean isStudio = DirectUtils.isStudio(competition);
        
        this.viewData = DataProvider.getContestIssues(contestId);

        // Set contest stats
        ContestStatsDTO contestStats = DirectUtils.getContestStats(currentUser, contestId, competition);
        getViewData().setContestStats(contestStats);

        // Set projects data
        List<ProjectBriefDTO> projects = DataProvider.getUserProjects(currentUser.getUserId());
        UserProjectsDTO userProjectsDTO = new UserProjectsDTO();
        userProjectsDTO.setProjects(projects);
        getViewData().setUserProjects(userProjectsDTO);

        getViewData().setLastClosedFinalFix(DirectUtils.getLastClosedFinalFixPhase(getProjectServices(), getProjectId()));
        
        // Set current project contests
        List<TypedContestBriefDTO> contests = DataProvider.getProjectTypedContests(
                currentUser.getUserId(), contestStats.getContest().getProject().getId());
        this.sessionData.setCurrentProjectContests(contests);

        // Set current project context based on selected contest
        this.sessionData.setCurrentProjectContext(contestStats.getContest().getProject());
        this.sessionData.setCurrentSelectDirectProjectID(contestStats.getContest().getProject().getId());
        
        DirectUtils.setDashboardData(currentUser, contestId, viewData,
                getContestServiceFacade(), !isStudio);
        
        // clear the old temporary attachments
        new SessionFileStore(request.getSession(true)).getFileMap().clear();
    }


    /**
     * <p>Gets the current session associated with the incoming request from client.</p>
     *
     * @return a <code>SessionData</code> providing access to current session.
     */
    public SessionData getSessionData() {
        return this.sessionData;
    }

}
