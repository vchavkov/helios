<#-- @ftlvariable name="" type="com.spotify.helios.master.resources.DashboardView" -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Helios Dashboard</title>
    <link href="/assets/css/bootstrap.min.css" rel="stylesheet">
    <link href="/assets/css/dashboard.css" rel="stylesheet">
</head>
<body>

<div class="navbar navbar-inverse navbar-fixed-top" role="navigation">
    <div class="container-fluid">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse"
                    data-target=".navbar-collapse">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Helios</a>
        </div>
        <div class="navbar-collapse collapse">
            <ul class="nav navbar-nav navbar-right">
                <li><a href="#">Dashboard</a></li>
                <li><a href="#">Settings</a></li>
                <li><a href="#">Profile</a></li>
                <li><a href="#">Help</a></li>
            </ul>
        </div>
    </div>
</div>

<div class="container-fluid">
    <div class="row">
        <div class="col-sm-3 col-md-2 sidebar">
            <ul class="nav nav-sidebar">
                <li class="active"><a href="#">Overview</a></li>
                <li><a href="#">Hosts</a></li>
                <li><a href="#">Jobs</a></li>
            </ul>
        </div>
        <div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
            <h1 class="page-header">Overview</h1>

            <h2 class="sub-header">Jobs</h2>
            <div class="table-responsive">
                <table class="table table-striped">
                    <thead>
                    <tr>
                        <th>Job Id</th>
                        <th>Hosts</th>
                        <th>Command</th>
                        <th>Environment</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list jobs as job>
                    <tr>
                        <td>${job.id?html}</td>
                        <td>${job.hosts}</td>
                        <td>${job.command?html}</td>
                        <td>${job.env?html}</td>
                    </tr>
                    </#list>
                    </tbody>
                </table>
            </div>

            <h2 class="sub-header">Hosts</h2>
            <div class="table-responsive">
                <table class="table table-striped">
                    <thead>
                    <tr>
                        <th>Host</th>
                        <th>Status</th>
                        <th>Deployed</th>
                        <th>Running</th>
                        <th>CPUs</th>
                        <th>Mem</th>
                        <th>Load Avg</th>
                        <th>Mem Usage</th>
                        <th>OS</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list hosts as host>
                    <tr>
                        <td>${host.name?html}</td>
                        <td>${host.status?html}</td>
                        <td>${host.jobsDeployed}</td>
                        <td>${host.jobsRunning}</td>
                        <td>${host.cpus}</td>
                        <td>${host.mem}</td>
                        <td>${host.loadAvg}</td>
                        <td>${host.memUsage}</td>
                        <td>${host.osName} ${host.osVersion}</td>
                    </tr>
                    </#list>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>


<!-- Bootstrap core JavaScript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<script src="/assets/js/jquery-2.1.0.min.js"></script>
<script src="/assets/js/bootstrap.min.js"></script>
</body>
</html>