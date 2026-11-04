{{/* Коротке ім'я чарта */}}
{{- define "hotel-booking.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/* Повне ім'я: будується з імені релізу, тому чарт можна встановити кілька разів */}}
{{- define "hotel-booking.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/* Назва та версія чарта для мітки helm.sh/chart */}}
{{- define "hotel-booking.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/* Ім'я об'єкта компонента: <повне ім'я>-<компонент>. Аргументи: dict "root" $ "component" "<ім'я>" */}}
{{- define "hotel-booking.componentName" -}}
{{- printf "%s-%s" (include "hotel-booking.fullname" .root) .component | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/* Мітки для селекторів (незмінні між оновленнями). Аргументи: dict "root" $ "component" "<ім'я>" */}}
{{- define "hotel-booking.selectorLabels" -}}
app.kubernetes.io/name: {{ include "hotel-booking.name" .root }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
app.kubernetes.io/component: {{ .component }}
{{- end }}

{{/* Повний набір міток. Аргументи: dict "root" $ "component" "<ім'я>" */}}
{{- define "hotel-booking.labels" -}}
helm.sh/chart: {{ include "hotel-booking.chart" .root }}
{{ include "hotel-booking.selectorLabels" . }}
app.kubernetes.io/version: {{ .root.Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .root.Release.Service }}
app.kubernetes.io/part-of: hotel-booking-system
{{- end }}

{{/* Образ застосунку: репозиторій + тег (власний тег, інакше image.tag, інакше appVersion). Аргументи: dict "root" $ "image" <мапа образу> */}}
{{- define "hotel-booking.image" -}}
{{- $tag := default (default .root.Chart.AppVersion .root.Values.image.tag) .image.tag -}}
{{- printf "%s:%s" .image.repository $tag }}
{{- end }}

{{/* Образ із фіксованим тегом (postgres, kafka, busybox). Аргумент: мапа з repository і tag */}}
{{- define "hotel-booking.imageRef" -}}
{{- printf "%s:%s" .repository (toString .tag) }}
{{- end }}
