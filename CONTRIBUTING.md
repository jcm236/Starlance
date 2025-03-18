# Starlance Contributing Guidelines

> ### Legal Notice <!-- omit in toc -->
> When contributing to this project, you must agree that you have authored 100% of the content, that you have the necessary rights to the content and that the content you contribute may be provided under the project license. (GNU-GPL 3.0)

### Steps
**1. Pick an issue**

Pick an issue you are interested in dealing with. If you want to deal with multiple issues, consider multiple PR's

**3. Make your changes**

Make sure you're following the style guide below in your code

**4. Open a pull request**

Don't forget to mention the issue!

**5. Wait for the pull request to be reviewed**

Should take from anywhere between 10 minutes to 2 weeks :P

Make sure to check back every so often to make sure there aren't any questions
we have that need answering before we can merge!

## Styleguide
### Commit Messages
A clear title, that summarises the changes done in that commit.
Then, you can add extra info further in the commit message.

**Examples:**
> Added thruster placement check

> Refactored teleporting
> 
> Changed it from using TeleportUtils to TeleportHandler

**Bad commits:**

_Though we are also guilty of having some like these in the past..._

> Fixed the thrusters

> Added stuff to magnets

### Java code
These conventions are not completely set in stone, and are mainly guidelines.
However, if you do stick to them, it will help make the Starlance codebase more consistent and easier for everyone involved.

**General:**
- 1 tab for indentation
- CamelCase class names (Java usually uses this anyway)
- CamelCase variable and function names
- Avoid large amounts of empty lines in a row
- Comments should have a leading space. `// Comment` instead of `//Comment`

**Naming:**
- Function names should easily explain what it does, javadoc should be used if a longer explanation is needed
- Variable names should describe what they store, not what they’re used for. E.g. "targetDimension" instead of "dimensionGoingTo"
- `static final` variables should have all caps names (e.g. `public static final MODID` not `public static final modid`)

**Classes:**
- The class or interface itself should be at the top, private classes and interfaces should go at the bottom
- For class variables, define `static` variables, then `instance` variables. Use the order:
    - `public`
    - `protected`
    - `private`

**Functions:**
- Functions don't need to be sorted, simple use an order that is easiest to look through.
  (So overloads and related functions should go next to each other)
- If a function is only used in one class, no need to put it in a util class. It can always be moved later/
- Empty functions should be defined as
```java
public static void myFunction() {}
```
instead of 
```java
public static void myFunction() {
    
    
}
```

**Code:**

Avoid multiple casts on one line, it can get confusing. Use multiple lines, or multiple variables, or both.
For example, use:
```java
OtherClass otherClassArg = (OtherClass) arg;
ExtraClass extraClassArgTwo = (ExtraClass) argTwo;
AClass aclassValue =  (AClass) utils.getThing(otherClassArg, extraClassArgTwo);
MyClass variable = (MyClass) (aclassValue + new AClass());
```
instead of
```java
MyClass variable = (MyClass) ((AClass) utils.getThing((OtherClass) arg, (ExtraClass) argTwo) + new AClass());
```

---

Use newlines when chain-calling for clarity. For example, use:
```java
Stream.of(xxx)
    .aVeryLongFunction(
            arg,
            argTwo
    )
    .b()
    .c();
```
instead of:
```java
Stream.of(xxx).aVeryLongFunction(arg, argTwo).b().c();
```

---

If you return in an IF statement, don't use `else` or `elseif` after it. For example, use:
```java
public static String myFunc(String arg) {
    if (arg.equals("thing")) {
        return "a";
    }
    
    if (arg.equals("thingTwo")) {
        return "b";
    }
    
    return "c";
}
```
Instead of:
```java
public static String myFunc(String arg) {
    if (arg.equals("thing")) {
        return "a";
    } else if (arg.equals("thingTwo")) {
        return "b";
    } else {
        return "c";
    }
}
```

---

If you want to continue a function only if a condition is met, use an inverted IF. For example, use:
```java
public static void myFunc(String arg) {
    if (!arg.equals("thing")) {
        return;
    }
    // Do stuff
}
```
instead of
```java
public static void myFunc(String arg) {
    if (arg.equals("thing")) {
        // Do stuff
    } else {
        return;
    }
}
```

---

If your function arguments are long, and you choose to write them on a newline,
don't leave the first argument on the first line. For example, use:
```java
public void sayHello(
  String aStr,
  Item anItem
) {
  // ...
};

sayHello(
  "a str",
  Items.WHAT_EVER,
);
```
instead of:
```java
public void sayHello(String heyCanYouSeeMe,
  Item anItem
) {
  // ...
}

sayHello("hi",
  Items.WHAT_EVER,
);
```

---

When using a lambda, always use `{}` and newlines for clarity. For example, use:
```java
myFunctionThatTakesALambda((a) -> {
    System.out.println("1");
    System.out.println("2");
});
```
instead of
```java
myFunctionThatTakesALambda((a) -> System.out.println("1");System.out.println("2"););
```
(Brackets `()` around single argument Lambda's are optional, and up to personal preference)
