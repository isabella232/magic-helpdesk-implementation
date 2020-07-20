#
# This file is used to define TLS Server Certificates used by various AWS resources

#    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *
#
# Certificates
#
#    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *

# magic-helpdesk-integration.web.bas.ac.uk
#
# This resource implicitly depends on the 'aws_s3_bucket.magic-helpdesk-integration' resource
# This resource relies on the AWS Terraform provider ('us-east-1' alias) being previously configured
#
# AWS source: http://docs.aws.amazon.com/acm/latest/userguide/acm-overview.html
# Terraform source: https://www.terraform.io/docs/providers/aws/r/acm_certificate.html
resource "aws_acm_certificate" "magic-helpdesk-integration" {
  provider = aws.us-east-1

  domain_name       = aws_s3_bucket.magic-helpdesk-integration.bucket
  validation_method = "DNS"

  tags = {
    Name         = "magic-helpdesk-integration.web.bas.ac.uk"
    X-Project    = "MAGIC Helpdesk"
    X-Managed-By = "Terraform"
  }
}

# magic-helpdesk.web.bas.ac.uk
#
# This resource implicitly depends on the 'aws_s3_bucket.magic-helpdesk-production' resource
# This resource relies on the AWS Terraform provider ('us-east-1' alias) being previously configured
#
# AWS source: http://docs.aws.amazon.com/acm/latest/userguide/acm-overview.html
# Terraform source: https://www.terraform.io/docs/providers/aws/r/acm_certificate.html
resource "aws_acm_certificate" "magic-helpdesk-production" {
  provider = aws.us-east-1

  domain_name       = aws_s3_bucket.magic-helpdesk-production.bucket
  validation_method = "DNS"

  tags = {
    Name         = "magic-helpdesk.web.bas.ac.uk"
    X-Project    = "MAGIC Helpdesk"
    X-Managed-By = "Terraform"
  }
}

#    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *
#
# Certificate validation records (Route53)
#
#    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *

# magic-helpdesk-integration.web.bas.ac.uk
#
# This resource implicitly depends on the 'aws_acm_certificate.magic-helpdesk-integration' resource
# This resource explicitly depends on outputs from the the 'terraform_remote_state.BAS-CORE-DOMAINS' data source
# This resource relies on the AWS Terraform provider being previously configured
#
# AWS source: http://docs.aws.amazon.com/Route53/latest/DeveloperGuide/rrsets-working-with.html
# Terraform source: https://www.terraform.io/docs/providers/aws/r/route53_record.html
#
# Tags are not supported by this resource
resource "aws_route53_record" "magic-helpdesk-integration-cert" {
  zone_id = data.terraform_remote_state.BAS-CORE-DOMAINS.outputs.WEB-BAS-AC-UK-ID

  name = aws_acm_certificate.magic-helpdesk-integration.domain_validation_options.0.resource_record_name
  type = aws_acm_certificate.magic-helpdesk-integration.domain_validation_options.0.resource_record_type
  ttl  = 60

  records = [
    aws_acm_certificate.magic-helpdesk-integration.domain_validation_options.0.resource_record_value,
  ]
}

# magic-helpdesk.web.bas.ac.uk
#
# This resource implicitly depends on the 'aws_acm_certificate.magic-helpdesk-production' resource
# This resource explicitly depends on outputs from the the 'terraform_remote_state.BAS-CORE-DOMAINS' data source
# This resource relies on the AWS Terraform provider being previously configured
#
# AWS source: http://docs.aws.amazon.com/Route53/latest/DeveloperGuide/rrsets-working-with.html
# Terraform source: https://www.terraform.io/docs/providers/aws/r/route53_record.html
#
# Tags are not supported by this resource
resource "aws_route53_record" "magic-helpdesk-production-cert" {
  zone_id = data.terraform_remote_state.BAS-CORE-DOMAINS.outputs.WEB-BAS-AC-UK-ID

  name = aws_acm_certificate.magic-helpdesk-production.domain_validation_options.0.resource_record_name
  type = aws_acm_certificate.magic-helpdesk-production.domain_validation_options.0.resource_record_type
  ttl  = 60

  records = [
    aws_acm_certificate.magic-helpdesk-production.domain_validation_options.0.resource_record_value,
  ]
}

#    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *
#
# Certificate validations
#
#    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *    *

# magic-helpdesk-integration.web.bas.ac.uk
#
# This resource may take a significant time (~30m) to create whilst domain validation is completed
#
# This resource implicitly depends on the 'aws_acm_certificate.magic-helpdesk-integration' resource
# This resource implicitly depends on the 'aws_route53_record.magic-helpdesk-integration-cert' resource
# This resource relies on the AWS Terraform provider ('us-east-1' alias) being previously configured
#
# AWS source: https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html
# Terraform source: https://www.terraform.io/docs/providers/aws/r/acm_certificate_validation.html
#
# Tags are not supported by this resource
resource "aws_acm_certificate_validation" "magic-helpdesk-integration" {
  provider = aws.us-east-1

  certificate_arn         = aws_acm_certificate.magic-helpdesk-integration.arn
  validation_record_fqdns = [aws_route53_record.magic-helpdesk-integration-cert.fqdn]
}

# magic-helpdesk.web.bas.ac.uk
#
# This resource may take a significant time (~30m) to create whilst domain validation is completed
#
# This resource implicitly depends on the 'aws_acm_certificate.magic-helpdesk-production' resource
# This resource implicitly depends on the 'aws_route53_record.magic-helpdesk-production-cert' resource
# This resource relies on the AWS Terraform provider ('us-east-1' alias) being previously configured
#
# AWS source: https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html
# Terraform source: https://www.terraform.io/docs/providers/aws/r/acm_certificate_validation.html
#
# Tags are not supported by this resource
resource "aws_acm_certificate_validation" "magic-helpdesk-production" {
  provider = aws.us-east-1

  certificate_arn         = aws_acm_certificate.magic-helpdesk-production.arn
  validation_record_fqdns = [aws_route53_record.magic-helpdesk-production-cert.fqdn]
}
