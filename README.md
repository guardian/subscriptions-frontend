# Subscriptions frontend

## General Setup
1. Go to project root
1. Run `./setup.sh` to install project-specific client-side dependencies.
1. Add the following to your `/etc/hosts`

   ```
   127.0.0.1   sub.thegulocal.com
   ```

1. Run `./nginx/setup.sh`
1. Download our private keys from the `subscriptions-private` S3 bucket. You will need an AWS account so ask another dev.

    If you have the AWS CLI set up you can run
    ```
    aws s3 cp s3://subscriptions-private/DEV/subscriptions-frontend.conf /etc/gu

    ```
1. Setup AWS credentials (we use the gu-membership account)

   Ask your teammate to create an account for you and securely send you the access key. For security, you must enable [MFA](http://aws.amazon.com/iam/details/mfa/).

   In `~/.aws/credentials` add the following:

   ```
   [membership]
   aws_access_key_id=[YOUR_AWS_ACCESS_KEY]
   aws_secret_access_key=[YOUR_AWS_SECRET_ACCESS_KEY]
   ```

   In `~/.aws/config` add the following:

   ```
   [default]
   output = json
   region = eu-west-1
   ```
1. Developer builds ``` sbt devrun ``` and navigate to ```sub.thegulocal.com```

## Javascript development

By default, the setup script will hash file assets and generate a `conf/assets.map` file,
which in turn will cause Play to render assets with their hashed path. Use the `grunt compile --dev`
task in order to have Play to render assets without hashing them. Finally, the grunt file provides a
convenient `watch` task, which will dynamically recompile assets as they get edited.

## Automated tests

The unit test suite can be run using the `sbt fast-test` task. This is just a convenient alias to execute all
tests except for those tagged as `acceptance`. The acceptance test suite can be instead run by executing
`sbt acceptance-test`. This assumes that an instance of the application is running locally at `https://sub.thegulocal.com`
