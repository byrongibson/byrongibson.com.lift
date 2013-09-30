package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
//import js.jquery.JQueryArtifacts
import sitemap._
import Loc._
import mapper._

import code.model._


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = 
          new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
          Props.get("db.url") openOr "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
          Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    Schemifier.schemify(true, Schemifier.infoF _, User)

    // where to search snippet
    LiftRules.addToPackages("code")
    
    // simple sitemap
    val entries = List(Menu("Home") / "index",
      Menu("Page 1") / "page1",
      Menu("Page 2") / "page2",
      Menu("Page 3") / "#"  >> PlaceHolder submenus (
        Menu("Page 3a") / "page3a" ,  
        Menu("Page 3b") / "page3b" ,
        Menu("Page 3c") / "page3c"))

    def sitemap = SiteMap(entries: _*)
    LiftRules.setSiteMap(sitemap)


    // Complex SiteMap
    /*def sitemap = SiteMap(
      Menu.i("Home") / "index" >> User.AddUserMenusAfter, // the simple way to declare a menu

      // more complex because this menu allows anything in the
      // /static path to be visible
      Menu(Loc("Static", Link(List("static"), true, "/static/index"), 
	       "Static Content")))

    def sitemapMutators = User.sitemapMutator

    def userLinkText = User.currentUser.map(_.shortName).openOr("not logged in").toString
 
    val entries = List(Menu("Home") / "index" >> LocGroup("main"),
        Menu("Page 1") / "page1" >> LocGroup("main"),
        Menu("Page 2") / "page2" >> LocGroup("main"),
        Menu("Page 3") / "page3" >> LocGroup("main") >> PlaceHolder submenus (
            Menu("Page 3a") / "page3a" ,  
            Menu("Page 3b") / "page3b" ,
            Menu("Page 3c") / "page3c") ,
        User.loginMenuLoc.open_!,
        User.createUserMenuLoc.open_!,
        Menu("user",userLinkText)  / "#" >> 
          MustBeLoggedIn >> LocGroup("user") >> PlaceHolder submenus (
              User.logoutMenuLoc.open_!,
              User.editUserMenuLoc.open_!,
              User.changePasswordMenuLoc.open_!
                )
        )


    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemapMutators(sitemap))
    */

    //Init the jQuery module, see http://liftweb.net/jquery for more information.
    //To use jQueryModule, uncomment these lines, the jQuery import line at the top,
    //and the jQuery liftmodule line in build.sbt.  Or just drop your own jQuery
    //into /src/main/webapp/scripts/vendor
    //JQueryModule.InitParam.JQuery=JQueryModule.JQuery172
    //JQueryModule.init()

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)
  }
}
