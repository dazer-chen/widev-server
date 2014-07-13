widev-server-v2
===============

New version of the Widev Server Developped with Play Framework (Scala)

## Pre-requires

1. HEROKU
	Install the [Heroku Toolbelt](https://toolbelt.heroku.com/) (Used to manage your application from your computer). 

## Deploying on HEROKU

```git push heroku master``` # update the Git repository used by Heroku.

```heroku ps``` # Check Active Instance on Heroku

## Play Commands

```activator run (-Dhttp.port=0000)``` # run your application to a development environment
```activator start``` # run your application to a production environment


## IDE Configuration 

```activator idea``` # Create an IDEA Structure