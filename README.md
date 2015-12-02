# Subscriptions frontend

### NGinx

   To run standalone you can use the default nginx installation as follows:

   Install nginx:

   Mac OSX: `brew install nginx`

   Make sure you have a sites-enabled folder under your nginx home. This should be

   Mac OSX: `~/Developers/etc/nginx/sites-enabled` or `/usr/local/etc/nginx/`
   Make sure your nginx.conf (found in your nginx home) contains the following line in the `http{...}` block: `include sites-enabled/*`;

   Run: `./nginx/setup.sh`

## General Setup

1. Go to project root
1. Run `./setup.sh` to install project-specific client-side dependencies.
1. Add the following to your hosts file in `/etc/hosts`

   ```
   127.0.0.1   sub.thegulocal.com
   127.0.0.1   profile.thegulocal.com
   ```

1. Run `npm install -g bower`
   Bower is a command-line utility, a package manager that makes it easy to manage all the application’s front-end dependencies. It works by fetching and installing packages
1. Change the ownership of the 'gu' directory under 'etc' to current user.
   `$ sudo -i chown -R {username} /etc/gu`
1. Run `./nginx/setup.sh`
1. Setup AWS credentials (we use the gu-membership account). Ask your teammate to create an account for you and securely send you the access key. For security, you must enable [MFA](http://aws.amazon.com/iam/details/mfa/).

   In `~/.aws/credentials` add the following:

   ```
   [membership]
   aws_access_key_id = [YOUR_AWS_ACCESS_KEY]
   aws_secret_access_key = [YOUR_AWS_SECRET_ACCESS_KEY]
   ```

   In `~/.aws/config` add the following:

   ```
   [default]
   region = eu-west-1
   ```

1. Download our private keys from the `subscriptions-private` S3 bucket. If you have the AWS CLI set up you can run:

    ```
    aws s3 cp s3://subscriptions-private/DEV/subscriptions-frontend.conf /etc/gu  --profile membership
    ```

1. Run ``` sbt devrun ``` and navigate to ```sub.thegulocal.com```

## Client-side Development

By default, the setup script will hash file assets and generate a `conf/assets.map` file,
which in turn will cause Play to render assets with their hashed path. Use the `grunt compile --dev`
task in order to have Play to render assets without hashing them.

Finally, the grunt file provides a
convenient `watch` task, which will dynamically recompile assets as they get edited.

## Test execution

### Unit tests

`sbt fast-test`

This sbt alias executes all tests apart from those tagged as acceptance tests.

### Acceptance tests

1. Run local subscription-frontend: `sbt devrun`
2. Run local [frontend](https://github.com/guardian/frontend): `./sbt "project identity" idrun`
3. `sbt acceptance-test`

These are browser driving Selenium tests.

### All tests

Run local frontend and subscription-fronted, and then execute: `sbt test`

