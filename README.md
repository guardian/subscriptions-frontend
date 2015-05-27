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