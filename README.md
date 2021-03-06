Activator Template Server
=================================

A small [Play!](http://playframework.com) application that can act as an Activator Template server.

The application takes a Git repository through the UI and will store and serve the templates to the [activator](https://typesafe.com/activator) script.

To use this server as an activator template repository, you must be using the patched versions of the activator script, activator launcher jar & the activator template cache jar (all available at the following repositories).

- [activator-template-cache](https://github.com/grahamar/activator-template-cache)
- [activator](https://github.com/grahamar/activator)

Once you're using the patched activator you must provide a repositories.properties file in your activator home (e.g. ~/.activator/1.0/repositories.properties)

repositories.properties should look like the following:

    privateRepo=http://localhost:9000/

Once all that's setup `activator list-templates` should display all the public templates published via [typesafe](https://typesafe.com/activator/template/contribute) and any private templates published to this server application.
