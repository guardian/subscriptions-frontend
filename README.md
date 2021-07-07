# Subscriptions frontend

## Nginx

#### Setup Nginx for identity-platform

Subscriptions depends on Nginx config and SSL certs from Identity, so you'll need to perform the
[Nginx setup for identity-platform](https://github.com/guardian/identity-platform/blob/master/README.md#setup-nginx-for-local-development)
first, before you do anything else.

   Run: `./nginx/setup.sh` and enter your password where prompted.

#### Setup Nginx for subscriptions-frontend

1. Install dependencies `brew bundle`
2. Run `nginx/setup.sh`

## General Setup

1. Get Janus credentials
1. Go to project root
1. Install [nvm](https://github.com/creationix/nvm#install-script), then run `nvm use`.
1. Run `./setup.sh` to install project-specific client-side dependencies.

1. Change the ownership of the 'gu' directory under 'etc' to current user.
   `$ sudo -i chown -R {username} /etc/gu`

   In `~/.aws/config` add the following:

   ```
   [default]
   region = eu-west-1
   ```

1. Download our private keys from the `gu-reader-revenue-private` S3 bucket. If you have the AWS CLI set up you can run:

    ```
    aws s3 cp s3://gu-reader-revenue-private/subscriptions/frontend/DEV/subscriptions-frontend.private.conf /etc/gu  --profile membership
    ```

1. Run ``` sbt devrun ``` and navigate to ```sub.thegulocal.com```

## Client-side Development

By default, the setup script will hash file assets and generate a `conf/assets.map` file,
which in turn will cause Play to render assets with their hashed path. Use the `grunt compile --dev`
task in order to have Play to render assets without hashing them.

Then run `yarn watch`, to dynamically recompile assets as they get edited.

## Test execution

### Unit tests

`sbt test`

