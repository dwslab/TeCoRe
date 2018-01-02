package controllers;

import play.mvc.*;

import play.twirl.api.Html;
import views.html.*;

public class HomeController extends Controller {

    public Result aboutTeCoRe() {
        return ok(index.render("About TeCoRe", views.html.about.render("tecore")));
    }

    public Result aboutTemporalConflictResolution() {
        return ok(index.render("About Temporal Conflict Resolution", Html.apply("")));
    }

    public Result aboutUs() {
        return ok(index.render("About Us", Html.apply("")));
    }

}
