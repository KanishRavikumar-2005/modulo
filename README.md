# Modulo – A Lightweight Java Web Framework

**Modulo** is a minimalistic and lightweight Java framework for building server-side web applications. It provides essential features like routing, session handling, and HTML rendering, all built with core Java—without external dependencies or complex configuration.

----------

## Overview

Modulo is designed for developers who want to build backend logic in Java without the overhead of Spring or Jakarta EE. With a focus on clarity and performance, Modulo is ideal for small web applications, prototypes, internal tools, or learning purposes.

----------

## Features

-   Simple and explicit routing
    
-   Support for static and dynamic URL paths
    
-   Basic session management
    
-   Clean HTML rendering with variable injection
    
-   Zero dependencies – pure Java
    

----------

## Basic Routing Example

```java
router.add("/hello", (req, q) -> {
    return Renderer.render("hello", new String[]{"message"}, new String[]{"Hello, world!"});
});

```

This sets up a route at `/hello` and renders a view called `hello.html`, injecting a variable `message`.

----------

## Dynamic Routing Example

```java
router.add("/user/:name", (req, q) -> {
    String username = q.param("name");
    return Renderer.render("profile", new String[]{"username"}, new String[]{username});
});

```

In this example, a request to `/user/Alice` will capture `"Alice"` as the `name` parameter and render the `profile.html` page with `username` set to `"Alice"`.

----------

## Rendering Example

Assuming the view file `welcome.html` contains:

```html
<h1>Welcome, {{name}}!</h1>

```

You can render it using:

```java
router.add("/welcome", (req, q) -> {
    return Renderer.render("welcome", new String[]{"name"}, new String[]{"Visitor"});
});

```

At runtime, `{{name}}` in the HTML will be replaced with `Visitor`.

----------

## Documentation

Documentation will be released soon.  
It will include full API usage, advanced examples, and best practices.

----------

## License

This project is licensed under the MIT License.

