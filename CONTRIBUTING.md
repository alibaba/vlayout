# Contributing Guide

Thank you for your attention to this project. Any bug, doc, examples and suggestion is appreciated. Here are some suggestions for you to create Pull Requests or open Issues.

## Branch Management

```
master
 ↑
develop         <--- PR(bugfix/typo/3rd-PR)
 ↑ PR
{type}/{description}
```  
Branches

* `master` branch
    * `master` is the latest (pre-)release branch.
* `develop` branch
    * `develop` is the stable developing branch. [Github Release](https://help.github.com/articles/creating-releases/) is used to publish a (pre-)release version to `master` branch.
    * ***It's RECOMMENDED to commit bugfix or feature PR to `develop`***.
* `{action}/{description}` branch
    * The branch for a developing or bugfix
    *. **DO NOT commit any PR to such a branch**.

## Branch Name

```
{action}/{description}
```

* `{action}`:
	* `feature`: used for developing a new feature.
	* `bugfix`: used for fixing bugs.
* for example: `feature/add_flex_layouthelper`

## Commit Log


```
{action} {description}
```

* `{action}`
    * `add`
    * `update` or `bugfix`
    * `remove`
    * ...
* `{description}`
    * It's ***RECOMMENDED*** to close issue with syntax `#123`, see [the doc](https://help.github.com/articles/closing-issues-via-commit-messages/) for more detail. It's useful for responding issues and release flow.

for example:

* `add new layout helper`
* `fix #123, make compatible to recyclervew 25.2.0`
* `remove abc`

## Issue

* Please apply a proper label to an issue.
* Suggested to use English.
* Provide sufficient instructions to be able to reproduce the issue and make the issues clear. Such as phone model, system version, sdk version, crash logs and screen captures. 

## Pull Request And Contributor License Agreement


[Create Pull Requests](https://github.com/alibaba/vlayout/compare) here.

In order to contribute code to vlayout, you (or the legal entity you represent) must sign the Contributor License Agreement (CLA).

You can read and sign the [Alibaba CLA](https://cla-assistant.io/alibaba/vlayout) online.

For CLA assistant service works properly, please make sure you have added email address that your commits linked to GitHub account.

## Code Style Guide

### Java & Android 

* Use [Google Java Style](https://google.github.io/styleguide/javaguide.html) as basic guidelines of java code.
* Follow [AOSP Code Style](https://source.android.com/source/code-style.html) for rest of android related code style.
