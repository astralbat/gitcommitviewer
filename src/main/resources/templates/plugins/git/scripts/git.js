jQuery(function($) {
    var git = {
        getContextPath : function() {
            return contextPath;
        },

        getParamsFromFieldSet : function(fieldSet) {
            var params = {};

            fieldSet.find("input").each(function() {
                params[this.name] = this.value;
            });

            return params;
        },

        initShowMoreButton : function(moreButton, moreUrlPath, dataType, callback) {
            if(moreButton.data('initialised') === undefined)
            {
                var moreButtonContainer = moreButton.parent();
                var params = this.getParamsFromFieldSet(moreButtonContainer.children("fieldset"));

                moreButton.click(function(event) {
                    moreButton.hide();
                    moreButtonContainer.append("<img src='" + git.getContextPath() + "/images/icons/wait.gif'/>");

                    $.ajax({
                        type : "GET",
                        url : git.getContextPath() + moreUrlPath,
                        dataType: dataType,
                        data : params,
                        success : function(someHtml) {
                            moreButtonContainer.remove();
                            callback(someHtml);

                        }
                    });
                    event.preventDefault();
                    event.stopPropagation();
                    return false;
                });
            }
        },

        createTemporaryInvisibleDiv : function() {
            return $(document.createElement("div")).hide();
        },

        initShowMoreButtonInIssueTab : function(moreButton) {
            var currentIssueKey = $("a[id^='issue_key_']").text() ||  $("#key-val").text();
            if (currentIssueKey)
                moreButton.parent().children("fieldset").find("input[name='issueKey']").attr("value", currentIssueKey);

                this.initShowMoreButton(
                    moreButton,
                    "/browse/" + currentIssueKey,
                    "html",
                    function(html) {
                        var issueActionsContainer = $("#issue_actions_container");
                        var tempCommitsDiv = git.createTemporaryInvisibleDiv();

                        tempCommitsDiv.html(html);
                        tempCommitsDiv.find("div.issuePanelContainer table, div.issuePanelContainer div.plugin_git_showmore_issuetab").appendTo(issueActionsContainer);

                        git.initShowMoreButtonInIssueTab(issueActionsContainer.find("input.plugin_git_showmore_issuetab_button"));
                    }
                );

            moreButton.data('initialised', true);
        },

        initShowMoreButtonInProjectTab : function(moreButton) {
            var selectedVersion = $("select[name='selectedVersion'] option:selected");
            var buttonFieldSet = moreButton.parent().children("fieldset");
            var params = this.getParamsFromFieldSet(buttonFieldSet);

            if (selectedVersion.length == 1) {
                buttonFieldSet.find("input[name='versionId']").attr("value", selectedVersion.attr("value"));
            }

            this.initShowMoreButton(
                moreButton,
                "/browse/" + params["projectKey"],
                "json",
                function(jsObject) {
                    var projectCommitsContainer = $("div.projectPanel table.plugin_git_projectcommits_table");
                    var tempCommitsDiv = git.createTemporaryInvisibleDiv();

                    tempCommitsDiv.html(jsObject["content"]);

                    var commitsHtml = tempCommitsDiv.find("table.plugin_git_projectcommits_table");
                    var commitsHtmlTableBody = commitsHtml.children("tbody");
                    
                    if (commitsHtmlTableBody.length > 0)
                        commitsHtml = commitsHtmlTableBody;

                    commitsHtml.appendTo(projectCommitsContainer);

                    git.initShowMoreButtonInProjectTab(projectCommitsContainer.find("input.plugin_git_showmore_projectab_button"));
                }
            );
        },

        initVersionSelectForm : function(theForm) {
            theForm.find("select[name='selectedVersion']").change(function() {
                theForm.submit();
            });
        }
    };

    if (typeof(JIRA.ViewIssueTabs) != "undefined") {
    	JIRA.ViewIssueTabs.onTabReady(function() {
    		$("input.plugin_git_showmore_issuetab_button").each(function() {
    			git.initShowMoreButtonInIssueTab($(this));
    		});
    	});
    }

    $(".plugin_git_showmore_issuetab_button").click(function() {
        $(this).data('initialised', false);
    });

    $("input.plugin_git_showmore_projectab_button").each(function() {
        git.initShowMoreButtonInProjectTab($(this));
    });

    git.initVersionSelectForm($("form.plugin_git_versionselect_form"));
});